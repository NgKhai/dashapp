/**
 * Delivery Routes
 * 
 * Endpoints for creating, managing, and tracking deliveries
 */

const express = require('express');
const router = express.Router();
const { supabaseAdmin } = require('../config/supabase');
const { success, error, notFound, forbidden } = require('../utils/response');
const { verifyToken, requireCustomer, requireDriver } = require('../middleware/auth');

// ============================================
// CUSTOMER DELIVERY ROUTES
// ============================================

/**
 * POST /deliveries
 * Create a new delivery (customer only)
 */
router.post('/', verifyToken, requireCustomer, async (req, res) => {
    try {
        const {
            pickup_address, pickup_lat, pickup_lng,
            drop_off_address, drop_off_lat, drop_off_lng,
            vehicle_type,
            notes,
            items,              // [NEW] JSON list of items
            items_photo_url,    // [NEW] Photo URL
            requires_loading_help // [NEW] Boolean
        } = req.body;

        // Validate required fields
        if (!pickup_lat || !pickup_lng || !drop_off_lat || !drop_off_lng) {
            return error(res, 'Pickup/Drop-off coordinates are required');
        }

        // Calculate distance (simple Haversine formula)
        const distance_km = calculateDistance(
            pickup_lat, pickup_lng,
            drop_off_lat, drop_off_lng
        );

        // Calculate price (simple logic for now)
        // Base price: 20,000 VND
        // Per km: 5,000 VND
        // Loading help: +50,000 VND
        let total_price = 20000 + (Math.ceil(distance_km) * 5000);

        if (requires_loading_help) {
            total_price += 50000;
        }

        // Pre-compute route from OSRM — store encoded polyline so mobile never calls OSRM
        let route_encoded = null;
        try {
            const osrmUrl = `http://router.project-osrm.org/route/v1/driving/${pickup_lng},${pickup_lat};${drop_off_lng},${drop_off_lat}?overview=full&geometries=polyline`;
            const osrmRes = await fetch(osrmUrl, { signal: AbortSignal.timeout(8000) });
            if (osrmRes.ok) {
                const osrmData = await osrmRes.json();
                if (osrmData.code === 'Ok' && osrmData.routes?.length > 0) {
                    route_encoded = osrmData.routes[0].geometry; // encoded polyline string
                }
            }
        } catch (osrmErr) {
            // Non-fatal: delivery is still created, mobile falls back to straight line
            console.warn('OSRM fetch failed during delivery creation:', osrmErr.message);
        }

        // Create delivery
        const { data: delivery, error: insertError } = await supabaseAdmin
            .from('deliveries')
            .insert({
                customer_id: req.customer.customer_id,
                pickup_address,
                pickup_lat,
                pickup_lng,
                drop_off_address,
                drop_off_lat,
                drop_off_lng,
                distance_km: Math.round(distance_km * 10) / 10,
                total_price: Math.round(total_price),
                vehicle_type: vehicle_type || 'MOTORCYCLE',
                status: 'PENDING',
                notes,
                items: items || [],
                items_photo_url,
                requires_loading_help: requires_loading_help || false,
                route_encoded              // null if OSRM timed out — mobile handles gracefully
            })
            .select()
            .single();

        if (insertError) {
            console.error('Create delivery db error:', insertError);
            return error(res, `Failed to create delivery: ${insertError.message}`);
        }

        // Create payment transaction (CASH)
        await supabaseAdmin
            .from('payment_transactions')
            .insert({
                delivery_id: delivery.delivery_id,
                amount: delivery.total_price,
                payment_method: 'CASH',
                status: 'PENDING'
            });

        return success(res, delivery, 'Delivery created', 201);

    } catch (err) {
        console.error('Create delivery error:', err);
        return error(res, `Failed to create delivery: ${err.message}`, 500);
    }
});


/**
 * GET /deliveries/:id
 * Get delivery details (customer or driver)
 */
router.get('/:id', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;

        const { data: delivery, error: fetchError } = await supabaseAdmin
            .from('deliveries')
            .select(`
        *,
        customer:customers(customer_id, name, phone, avatar_url),
        driver:drivers(driver_id, name, phone, avatar_url, rating),
        vehicle:vehicles(*),
        rating:delivery_ratings(*),
        payment:payment_transactions(*)
      `)
            .eq('delivery_id', id)
            .single();

        if (fetchError || !delivery) {
            return notFound(res, 'Delivery not found');
        }

        return success(res, delivery, 'Delivery retrieved');

    } catch (err) {
        console.error('Get delivery error:', err);
        return error(res, 'Failed to get delivery', 500);
    }
});

/**
 * GET /deliveries/:id/track
 * Track a delivery — returns driver location and recent history
 */
router.get('/:id/track', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;

        // Get delivery with driver info
        const { data: delivery, error: fetchError } = await supabaseAdmin
            .from('deliveries')
            .select(`
                *,
                customer:customers(customer_id, name, phone),
                driver:drivers(driver_id, name, phone, current_lat, current_lng, last_location_update)
            `)
            .eq('delivery_id', id)
            .single();

        if (fetchError || !delivery) {
            return notFound(res, 'Delivery not found');
        }

        // Get recent location history (last 20 points)
        let locationHistory = [];
        if (delivery.driver_id) {
            const { data: locations } = await supabaseAdmin
                .from('driver_locations')
                .select('lat, lng, recorded_at')
                .eq('delivery_id', id)
                .order('recorded_at', { ascending: false })
                .limit(20);

            locationHistory = locations || [];
        }

        return success(res, {
            delivery,
            location_history: locationHistory
        }, 'Tracking data retrieved');

    } catch (err) {
        console.error('Track delivery error:', err);
        return error(res, 'Failed to track delivery', 500);
    }
});

// ============================================
// DRIVER DELIVERY ACTIONS
// ============================================

/**
 * PUT /deliveries/:id/accept
 * Accept a delivery (driver only)
 */
router.put('/:id/accept', verifyToken, requireDriver, async (req, res) => {
    try {
        const { id } = req.params;

        // Check if driver is online and verified
        if (!req.driver.is_online) {
            return forbidden(res, 'Driver must be online to accept deliveries');
        }

        if (!req.driver.is_verified) {
            return forbidden(res, 'Driver must be verified to accept deliveries');
        }

        // Get delivery
        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        if (delivery.status !== 'PENDING') {
            return error(res, 'Delivery is not available');
        }

        // Get driver's primary vehicle
        const { data: vehicleAssignment } = await supabaseAdmin
            .from('driver_vehicles')
            .select('vehicle_id')
            .eq('driver_id', req.driver.driver_id)
            .eq('is_primary', true)
            .single();

        // Accept delivery
        const { data: updatedDelivery, error: updateError } = await supabaseAdmin
            .from('deliveries')
            .update({
                driver_id: req.driver.driver_id,
                vehicle_id: vehicleAssignment?.vehicle_id || null,
                status: 'ACCEPTED',
                accepted_at: new Date().toISOString()
            })
            .eq('delivery_id', id)
            .eq('status', 'PENDING')  // Ensure still pending (race condition prevention)
            .select(`
        *,
        customer:customers(customer_id, name, phone)
      `)
            .single();

        if (updateError || !updatedDelivery) {
            return error(res, 'Failed to accept delivery (may already be taken)');
        }

        return success(res, updatedDelivery, 'Delivery accepted');

    } catch (err) {
        console.error('Accept delivery error:', err);
        return error(res, 'Failed to accept delivery', 500);
    }
});

/**
 * PUT /deliveries/:id/pickup
 * Confirm pickup (driver only)
 */
router.put('/:id/pickup', verifyToken, requireDriver, async (req, res) => {
    try {
        const { id } = req.params;

        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .eq('driver_id', req.driver.driver_id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        if (delivery.status !== 'ACCEPTED') {
            return error(res, 'Delivery must be in ACCEPTED status');
        }

        const { data: updatedDelivery, error: updateError } = await supabaseAdmin
            .from('deliveries')
            .update({
                status: 'PICKED_UP',
                picked_up_at: new Date().toISOString()
            })
            .eq('delivery_id', id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to update delivery');
        }

        return success(res, updatedDelivery, 'Pickup confirmed');

    } catch (err) {
        console.error('Pickup delivery error:', err);
        return error(res, 'Failed to confirm pickup', 500);
    }
});

/**
 * PUT /deliveries/:id/delivering
 * Start delivering (driver only)
 */
router.put('/:id/delivering', verifyToken, requireDriver, async (req, res) => {
    try {
        const { id } = req.params;

        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .eq('driver_id', req.driver.driver_id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        if (delivery.status !== 'PICKED_UP') {
            return error(res, 'Package must be picked up first');
        }

        const { data: updatedDelivery, error: updateError } = await supabaseAdmin
            .from('deliveries')
            .update({ status: 'DELIVERING' })
            .eq('delivery_id', id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to update delivery');
        }

        return success(res, updatedDelivery, 'Delivery in progress');

    } catch (err) {
        console.error('Delivering error:', err);
        return error(res, 'Failed to update delivery', 500);
    }
});

/**
 * PUT /deliveries/:id/complete
 * Complete delivery (driver only)
 */
router.put('/:id/complete', verifyToken, requireDriver, async (req, res) => {
    try {
        const { id } = req.params;

        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .eq('driver_id', req.driver.driver_id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        if (delivery.status !== 'DELIVERING' && delivery.status !== 'PICKED_UP') {
            return error(res, 'Delivery cannot be completed from current status');
        }

        // Complete delivery
        const { data: updatedDelivery, error: updateError } = await supabaseAdmin
            .from('deliveries')
            .update({
                status: 'COMPLETED',
                delivered_at: new Date().toISOString()
            })
            .eq('delivery_id', id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to complete delivery');
        }

        // Update payment status to COMPLETED (cash received)
        await supabaseAdmin
            .from('payment_transactions')
            .update({
                status: 'COMPLETED',
                paid_at: new Date().toISOString()
            })
            .eq('delivery_id', id);

        return success(res, updatedDelivery, 'Delivery completed');

    } catch (err) {
        console.error('Complete delivery error:', err);
        return error(res, 'Failed to complete delivery', 500);
    }
});

/**
 * PUT /deliveries/:id/cancel
 * Cancel delivery (customer or driver)
 */
router.put('/:id/cancel', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { reason } = req.body;

        // Get delivery
        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        // Determine who is cancelling
        let cancelled_by = null;

        // Check if user is customer
        const { data: customer } = await supabaseAdmin
            .from('customers')
            .select('customer_id')
            .eq('auth_user_id', req.user.id)
            .single();

        if (customer && delivery.customer_id === customer.customer_id) {
            cancelled_by = 'CUSTOMER';
        }

        // Check if user is driver
        const { data: driver } = await supabaseAdmin
            .from('drivers')
            .select('driver_id')
            .eq('auth_user_id', req.user.id)
            .single();

        if (driver && delivery.driver_id === driver.driver_id) {
            cancelled_by = 'DRIVER';
        }

        if (!cancelled_by) {
            return forbidden(res, 'You cannot cancel this delivery');
        }

        // Check if delivery can be cancelled
        if (['COMPLETED', 'CANCELLED'].includes(delivery.status)) {
            return error(res, 'Delivery cannot be cancelled');
        }

        // Cancel delivery
        const { data: updatedDelivery, error: updateError } = await supabaseAdmin
            .from('deliveries')
            .update({
                status: 'CANCELLED',
                cancelled_at: new Date().toISOString(),
                cancelled_by,
                cancellation_reason: reason || null
            })
            .eq('delivery_id', id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to cancel delivery');
        }

        // Update payment status
        await supabaseAdmin
            .from('payment_transactions')
            .update({ status: 'FAILED' })
            .eq('delivery_id', id);

        return success(res, updatedDelivery, 'Delivery cancelled');

    } catch (err) {
        console.error('Cancel delivery error:', err);
        return error(res, 'Failed to cancel delivery', 500);
    }
});

// ============================================
// RATING ROUTES
// ============================================

/**
 * POST /deliveries/:id/rate
 * Rate a delivery (customer rates driver, or driver rates customer)
 */
router.post('/:id/rate', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;
        const { rating, review } = req.body;

        if (!rating || rating < 1 || rating > 5) {
            return error(res, 'Rating must be between 1 and 5');
        }

        // Get delivery
        const { data: delivery } = await supabaseAdmin
            .from('deliveries')
            .select('*')
            .eq('delivery_id', id)
            .single();

        if (!delivery) {
            return notFound(res, 'Delivery not found');
        }

        if (delivery.status !== 'COMPLETED') {
            return error(res, 'Can only rate completed deliveries');
        }

        // Check who is rating
        const { data: customer } = await supabaseAdmin
            .from('customers')
            .select('customer_id')
            .eq('auth_user_id', req.user.id)
            .single();

        const { data: driver } = await supabaseAdmin
            .from('drivers')
            .select('driver_id')
            .eq('auth_user_id', req.user.id)
            .single();

        // Check if rating already exists
        const { data: existingRating } = await supabaseAdmin
            .from('delivery_ratings')
            .select('*')
            .eq('delivery_id', id)
            .single();

        if (customer && delivery.customer_id === customer.customer_id) {
            // Customer rating driver
            if (existingRating) {
                // Update existing rating
                const { data: updatedRating, error: updateError } = await supabaseAdmin
                    .from('delivery_ratings')
                    .update({
                        customer_rating: rating,
                        customer_review: review || null
                    })
                    .eq('delivery_id', id)
                    .select()
                    .single();

                if (updateError) {
                    return error(res, 'Failed to update rating');
                }

                return success(res, updatedRating, 'Rating updated');
            } else {
                // Create new rating
                const { data: newRating, error: insertError } = await supabaseAdmin
                    .from('delivery_ratings')
                    .insert({
                        delivery_id: id,
                        customer_rating: rating,
                        customer_review: review || null
                    })
                    .select()
                    .single();

                if (insertError) {
                    return error(res, 'Failed to create rating');
                }

                return success(res, newRating, 'Rating submitted', 201);
            }
        } else if (driver && delivery.driver_id === driver.driver_id) {
            // Driver rating customer
            if (existingRating) {
                const { data: updatedRating, error: updateError } = await supabaseAdmin
                    .from('delivery_ratings')
                    .update({
                        driver_rating: rating,
                        driver_review: review || null
                    })
                    .eq('delivery_id', id)
                    .select()
                    .single();

                if (updateError) {
                    return error(res, 'Failed to update rating');
                }

                return success(res, updatedRating, 'Rating updated');
            } else {
                const { data: newRating, error: insertError } = await supabaseAdmin
                    .from('delivery_ratings')
                    .insert({
                        delivery_id: id,
                        driver_rating: rating,
                        driver_review: review || null
                    })
                    .select()
                    .single();

                if (insertError) {
                    return error(res, 'Failed to create rating');
                }

                return success(res, newRating, 'Rating submitted', 201);
            }
        } else {
            return forbidden(res, 'You cannot rate this delivery');
        }

    } catch (err) {
        console.error('Rate delivery error:', err);
        return error(res, 'Failed to submit rating', 500);
    }
});

/**
 * GET /deliveries/:id/track
 * Get real-time tracking info for a delivery
 */
router.get('/:id/track', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;

        // Get delivery with driver location
        const { data: delivery, error: fetchError } = await supabaseAdmin
            .from('deliveries')
            .select(`
        delivery_id,
        status,
        pickup_lat,
        pickup_lng,
        drop_off_lat,
        drop_off_lng,
        driver:drivers(
          driver_id,
          name,
          phone,
          current_lat,
          current_lng,
          last_location_update
        )
      `)
            .eq('delivery_id', id)
            .single();

        if (fetchError || !delivery) {
            return notFound(res, 'Delivery not found');
        }

        // Get recent location history
        const { data: locationHistory } = await supabaseAdmin
            .from('driver_locations')
            .select('lat, lng, recorded_at')
            .eq('delivery_id', id)
            .order('recorded_at', { ascending: false })
            .limit(10);

        return success(res, {
            delivery,
            location_history: locationHistory || []
        }, 'Tracking info retrieved');

    } catch (err) {
        console.error('Track delivery error:', err);
        return error(res, 'Failed to get tracking info', 500);
    }
});

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Calculate distance between two coordinates using Haversine formula
 * @returns Distance in kilometers
 */
function calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371; // Earth's radius in km
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function toRad(deg) {
    return deg * (Math.PI / 180);
}

module.exports = router;
