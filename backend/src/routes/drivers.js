/**
 * Driver Routes
 * 
 * Endpoints for driver profile, status, location, and vehicle management
 */

const express = require('express');
const router = express.Router();
const { supabaseAdmin } = require('../config/supabase');
const { success, error, notFound } = require('../utils/response');
const { verifyToken, requireDriver } = require('../middleware/auth');

// All routes require authentication as driver
router.use(verifyToken, requireDriver);

// ============================================
// PROFILE ROUTES
// ============================================

/**
 * GET /drivers/profile
 * Get current driver's profile
 */
router.get('/profile', async (req, res) => {
    try {
        return success(res, req.driver, 'Profile retrieved');
    } catch (err) {
        console.error('Get profile error:', err);
        return error(res, 'Failed to get profile', 500);
    }
});

/**
 * PUT /drivers/profile
 * Update current driver's profile
 */
router.put('/profile', async (req, res) => {
    try {
        const { name, email, avatar_url } = req.body;

        const updateData = {};
        if (name) updateData.name = name;
        if (email !== undefined) updateData.email = email;
        if (avatar_url !== undefined) updateData.avatar_url = avatar_url;

        if (Object.keys(updateData).length === 0) {
            return error(res, 'No fields to update');
        }

        const { data: driver, error: updateError } = await supabaseAdmin
            .from('drivers')
            .update(updateData)
            .eq('driver_id', req.driver.driver_id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to update profile');
        }

        return success(res, driver, 'Profile updated');

    } catch (err) {
        console.error('Update profile error:', err);
        return error(res, 'Failed to update profile', 500);
    }
});

// ============================================
// STATUS & LOCATION ROUTES
// ============================================

/**
 * PUT /drivers/status
 * Toggle driver online/offline status
 */
router.put('/status', async (req, res) => {
    try {
        const { is_online } = req.body;

        if (is_online === undefined) {
            return error(res, 'is_online is required');
        }

        // Check if driver is verified before going online
        if (is_online && !req.driver.is_verified) {
            return error(res, 'Driver must be verified to go online', 403);
        }

        const { data: driver, error: updateError } = await supabaseAdmin
            .from('drivers')
            .update({
                is_online,
                last_location_update: new Date().toISOString()
            })
            .eq('driver_id', req.driver.driver_id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to update status');
        }

        return success(res, driver, `Driver is now ${is_online ? 'online' : 'offline'}`);

    } catch (err) {
        console.error('Update status error:', err);
        return error(res, 'Failed to update status', 500);
    }
});

/**
 * PUT /drivers/location
 * Update driver's current location
 */
router.put('/location', async (req, res) => {
    try {
        const { lat, lng, delivery_id } = req.body;

        if (lat === undefined || lng === undefined) {
            return error(res, 'lat and lng are required');
        }

        // Update driver's current location
        const { error: updateError } = await supabaseAdmin
            .from('drivers')
            .update({
                current_lat: lat,
                current_lng: lng,
                last_location_update: new Date().toISOString()
            })
            .eq('driver_id', req.driver.driver_id);

        if (updateError) {
            return error(res, 'Failed to update location');
        }

        // Also log to driver_locations history (for tracking during delivery)
        if (delivery_id) {
            await supabaseAdmin
                .from('driver_locations')
                .insert({
                    driver_id: req.driver.driver_id,
                    delivery_id,
                    lat,
                    lng
                });
        }

        return success(res, { lat, lng }, 'Location updated');

    } catch (err) {
        console.error('Update location error:', err);
        return error(res, 'Failed to update location', 500);
    }
});

// ============================================
// VEHICLE ROUTES
// ============================================

/**
 * GET /drivers/vehicles
 * Get driver's assigned vehicles
 */
router.get('/vehicles', async (req, res) => {
    try {
        const { data: vehicleAssignments, error: fetchError } = await supabaseAdmin
            .from('driver_vehicles')
            .select(`
        id,
        is_primary,
        assigned_at,
        vehicle:vehicles(*)
      `)
            .eq('driver_id', req.driver.driver_id);

        if (fetchError) {
            return error(res, 'Failed to fetch vehicles');
        }

        return success(res, vehicleAssignments, 'Vehicles retrieved');

    } catch (err) {
        console.error('Get vehicles error:', err);
        return error(res, 'Failed to get vehicles', 500);
    }
});

/**
 * PUT /drivers/vehicles/:id/primary
 * Set a vehicle as primary (currently active)
 */
router.put('/vehicles/:id/primary', async (req, res) => {
    try {
        const { id } = req.params;  // This is the driver_vehicles.id

        // Verify this assignment belongs to the driver
        const { data: assignment } = await supabaseAdmin
            .from('driver_vehicles')
            .select('id, vehicle_id')
            .eq('id', id)
            .eq('driver_id', req.driver.driver_id)
            .single();

        if (!assignment) {
            return notFound(res, 'Vehicle assignment not found');
        }

        // Unset all other vehicles as primary
        await supabaseAdmin
            .from('driver_vehicles')
            .update({ is_primary: false })
            .eq('driver_id', req.driver.driver_id);

        // Set selected vehicle as primary
        const { data: updated, error: updateError } = await supabaseAdmin
            .from('driver_vehicles')
            .update({ is_primary: true })
            .eq('id', id)
            .select(`
        id,
        is_primary,
        vehicle:vehicles(*)
      `)
            .single();

        if (updateError) {
            return error(res, 'Failed to set primary vehicle');
        }

        return success(res, updated, 'Primary vehicle set');

    } catch (err) {
        console.error('Set primary vehicle error:', err);
        return error(res, 'Failed to set primary vehicle', 500);
    }
});

// ============================================
// DELIVERY ROUTES
// ============================================

/**
 * GET /drivers/pending
 * Get available deliveries nearby (PENDING status)
 */
router.get('/pending', async (req, res) => {
    try {
        // Check if driver is online and verified
        if (!req.driver.is_online) {
            return error(res, 'Driver must be online to see pending deliveries', 403);
        }

        if (!req.driver.is_verified) {
            return error(res, 'Driver must be verified to see deliveries', 403);
        }

        const { limit = 10 } = req.query;

        // Get pending deliveries (in production, filter by distance too)
        const { data: deliveries, error: fetchError } = await supabaseAdmin
            .from('deliveries')
            .select(`
        *,
        customer:customers(customer_id, name, phone)
      `)
            .eq('status', 'PENDING')
            .order('created_at', { ascending: true })
            .limit(limit);

        if (fetchError) {
            return error(res, 'Failed to fetch pending deliveries');
        }

        return success(res, deliveries, 'Pending deliveries retrieved');

    } catch (err) {
        console.error('Get pending deliveries error:', err);
        return error(res, 'Failed to get pending deliveries', 500);
    }
});

/**
 * GET /drivers/deliveries
 * Get driver's delivery history
 */
router.get('/deliveries', async (req, res) => {
    try {
        const { status, limit = 20, offset = 0 } = req.query;

        let query = supabaseAdmin
            .from('deliveries')
            .select(`
        *,
        customer:customers(customer_id, name, phone),
        rating:delivery_ratings(customer_rating, customer_review)
      `)
            .eq('driver_id', req.driver.driver_id)
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (status) {
            query = query.eq('status', status.toUpperCase());
        }

        const { data: deliveries, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch deliveries');
        }

        return success(res, deliveries, 'Deliveries retrieved');

    } catch (err) {
        console.error('Get deliveries error:', err);
        return error(res, 'Failed to get deliveries', 500);
    }
});

/**
 * GET /drivers/earnings
 * Get driver's earnings summary
 */
router.get('/earnings', async (req, res) => {
    try {
        // Get completed deliveries for earnings calculation
        const { data: completedDeliveries, error: fetchError } = await supabaseAdmin
            .from('deliveries')
            .select('total_price, delivered_at')
            .eq('driver_id', req.driver.driver_id)
            .eq('status', 'COMPLETED');

        if (fetchError) {
            return error(res, 'Failed to fetch earnings');
        }

        // Calculate earnings (driver gets 80% of delivery price)
        const driverShare = 0.8;
        const totalEarnings = completedDeliveries.reduce((sum, d) => sum + (d.total_price * driverShare), 0);

        // Today's earnings
        const today = new Date().toISOString().split('T')[0];
        const todayEarnings = completedDeliveries
            .filter(d => d.delivered_at && d.delivered_at.startsWith(today))
            .reduce((sum, d) => sum + (d.total_price * driverShare), 0);

        return success(res, {
            total_deliveries: req.driver.total_deliveries,
            total_earnings: totalEarnings,
            today_earnings: todayEarnings,
            rating: req.driver.rating,
            total_ratings: req.driver.total_ratings
        }, 'Earnings retrieved');

    } catch (err) {
        console.error('Get earnings error:', err);
        return error(res, 'Failed to get earnings', 500);
    }
});

module.exports = router;
