/**
 * Admin Routes
 * 
 * Endpoints for admin dashboard and management
 */

const express = require('express');
const router = express.Router();
const { supabaseAdmin } = require('../config/supabase');
const { success, error, notFound } = require('../utils/response');
const { verifyToken, requireAdmin } = require('../middleware/auth');

// All routes require admin authentication
router.use(verifyToken, requireAdmin);

// ============================================
// DASHBOARD
// ============================================

/**
 * GET /admin/dashboard
 * Get dashboard statistics
 */
router.get('/dashboard', async (req, res) => {
    try {
        // Get counts
        const [
            { count: totalCustomers },
            { count: totalDrivers },
            { count: totalDeliveries },
            { count: pendingDeliveries },
            { count: activeDeliveries },
            { count: completedToday }
        ] = await Promise.all([
            supabaseAdmin.from('customers').select('*', { count: 'exact', head: true }),
            supabaseAdmin.from('drivers').select('*', { count: 'exact', head: true }),
            supabaseAdmin.from('deliveries').select('*', { count: 'exact', head: true }),
            supabaseAdmin.from('deliveries').select('*', { count: 'exact', head: true }).eq('status', 'PENDING'),
            supabaseAdmin.from('deliveries').select('*', { count: 'exact', head: true }).in('status', ['ACCEPTED', 'PICKED_UP', 'DELIVERING']),
            supabaseAdmin.from('deliveries').select('*', { count: 'exact', head: true })
                .eq('status', 'COMPLETED')
                .gte('delivered_at', new Date().toISOString().split('T')[0])
        ]);

        // Get online drivers
        const { count: onlineDrivers } = await supabaseAdmin
            .from('drivers')
            .select('*', { count: 'exact', head: true })
            .eq('is_online', true);

        // Get today's revenue
        const { data: todayDeliveries } = await supabaseAdmin
            .from('deliveries')
            .select('total_price')
            .eq('status', 'COMPLETED')
            .gte('delivered_at', new Date().toISOString().split('T')[0]);

        const todayRevenue = todayDeliveries?.reduce((sum, d) => sum + d.total_price, 0) || 0;

        return success(res, {
            customers: {
                total: totalCustomers || 0
            },
            drivers: {
                total: totalDrivers || 0,
                online: onlineDrivers || 0
            },
            deliveries: {
                total: totalDeliveries || 0,
                pending: pendingDeliveries || 0,
                active: activeDeliveries || 0,
                completed_today: completedToday || 0
            },
            revenue: {
                today: todayRevenue
            }
        }, 'Dashboard stats retrieved');

    } catch (err) {
        console.error('Dashboard error:', err);
        return error(res, 'Failed to get dashboard stats', 500);
    }
});

// ============================================
// CUSTOMER MANAGEMENT
// ============================================

/**
 * GET /admin/customers
 * List all customers
 */
router.get('/customers', async (req, res) => {
    try {
        const { limit = 50, offset = 0, search } = req.query;

        let query = supabaseAdmin
            .from('customers')
            .select('*', { count: 'exact' })
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (search) {
            query = query.or(`name.ilike.%${search}%,phone.ilike.%${search}%,email.ilike.%${search}%`);
        }

        const { data: customers, count, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch customers');
        }

        return success(res, { customers, total: count }, 'Customers retrieved');

    } catch (err) {
        console.error('List customers error:', err);
        return error(res, 'Failed to list customers', 500);
    }
});

// ============================================
// DRIVER MANAGEMENT
// ============================================

/**
 * GET /admin/drivers
 * List all drivers
 */
router.get('/drivers', async (req, res) => {
    try {
        const { limit = 50, offset = 0, search, is_verified, is_online } = req.query;

        let query = supabaseAdmin
            .from('drivers')
            .select('*', { count: 'exact' })
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (search) {
            query = query.or(`name.ilike.%${search}%,phone.ilike.%${search}%,email.ilike.%${search}%`);
        }

        if (is_verified !== undefined) {
            query = query.eq('is_verified', is_verified === 'true');
        }

        if (is_online !== undefined) {
            query = query.eq('is_online', is_online === 'true');
        }

        const { data: drivers, count, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch drivers');
        }

        return success(res, { drivers, total: count }, 'Drivers retrieved');

    } catch (err) {
        console.error('List drivers error:', err);
        return error(res, 'Failed to list drivers', 500);
    }
});

/**
 * PUT /admin/drivers/:id/verify
 * Verify a driver
 */
router.put('/drivers/:id/verify', async (req, res) => {
    try {
        const { id } = req.params;

        const { data: driver, error: updateError } = await supabaseAdmin
            .from('drivers')
            .update({ is_verified: true })
            .eq('driver_id', id)
            .select()
            .single();

        if (updateError || !driver) {
            return notFound(res, 'Driver not found');
        }

        return success(res, driver, 'Driver verified');

    } catch (err) {
        console.error('Verify driver error:', err);
        return error(res, 'Failed to verify driver', 500);
    }
});

/**
 * PUT /admin/drivers/:id/suspend
 * Suspend or unsuspend a driver
 */
router.put('/drivers/:id/suspend', async (req, res) => {
    try {
        const { id } = req.params;
        const { is_active } = req.body;

        if (is_active === undefined) {
            return error(res, 'is_active is required');
        }

        const { data: driver, error: updateError } = await supabaseAdmin
            .from('drivers')
            .update({
                is_active,
                is_online: is_active ? undefined : false  // Force offline if suspended
            })
            .eq('driver_id', id)
            .select()
            .single();

        if (updateError || !driver) {
            return notFound(res, 'Driver not found');
        }

        return success(res, driver, is_active ? 'Driver activated' : 'Driver suspended');

    } catch (err) {
        console.error('Suspend driver error:', err);
        return error(res, 'Failed to update driver', 500);
    }
});

// ============================================
// VEHICLE MANAGEMENT
// ============================================

/**
 * GET /admin/vehicles
 * List all vehicles
 */
router.get('/vehicles', async (req, res) => {
    try {
        const { limit = 50, offset = 0, type } = req.query;

        let query = supabaseAdmin
            .from('vehicles')
            .select('*', { count: 'exact' })
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (type) {
            query = query.eq('type', type.toUpperCase());
        }

        const { data: vehicles, count, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch vehicles');
        }

        return success(res, { vehicles, total: count }, 'Vehicles retrieved');

    } catch (err) {
        console.error('List vehicles error:', err);
        return error(res, 'Failed to list vehicles', 500);
    }
});

/**
 * POST /admin/vehicles
 * Add a new vehicle
 */
router.post('/vehicles', async (req, res) => {
    try {
        const { type, plate_number, model_name, color, capacity_kg } = req.body;

        if (!type || !plate_number || !model_name || !color) {
            return error(res, 'Type, plate_number, model_name, and color are required');
        }

        const { data: vehicle, error: insertError } = await supabaseAdmin
            .from('vehicles')
            .insert({
                type: type.toUpperCase(),
                plate_number,
                model_name,
                color,
                capacity_kg
            })
            .select()
            .single();

        if (insertError) {
            if (insertError.code === '23505') {
                return error(res, 'Plate number already exists');
            }
            return error(res, 'Failed to add vehicle');
        }

        return success(res, vehicle, 'Vehicle added', 201);

    } catch (err) {
        console.error('Add vehicle error:', err);
        return error(res, 'Failed to add vehicle', 500);
    }
});

/**
 * PUT /admin/vehicles/:id
 * Update a vehicle
 */
router.put('/vehicles/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { type, plate_number, model_name, color, capacity_kg, is_active } = req.body;

        const updateData = {};
        if (type) updateData.type = type.toUpperCase();
        if (plate_number) updateData.plate_number = plate_number;
        if (model_name) updateData.model_name = model_name;
        if (color) updateData.color = color;
        if (capacity_kg !== undefined) updateData.capacity_kg = capacity_kg;
        if (is_active !== undefined) updateData.is_active = is_active;

        const { data: vehicle, error: updateError } = await supabaseAdmin
            .from('vehicles')
            .update(updateData)
            .eq('vehicle_id', id)
            .select()
            .single();

        if (updateError || !vehicle) {
            return notFound(res, 'Vehicle not found');
        }

        return success(res, vehicle, 'Vehicle updated');

    } catch (err) {
        console.error('Update vehicle error:', err);
        return error(res, 'Failed to update vehicle', 500);
    }
});

/**
 * POST /admin/vehicles/:id/assign
 * Assign a vehicle to a driver
 */
router.post('/vehicles/:id/assign', async (req, res) => {
    try {
        const { id } = req.params;
        const { driver_id, is_primary } = req.body;

        if (!driver_id) {
            return error(res, 'driver_id is required');
        }

        // Check if assignment already exists
        const { data: existing } = await supabaseAdmin
            .from('driver_vehicles')
            .select('id')
            .eq('driver_id', driver_id)
            .eq('vehicle_id', id)
            .single();

        if (existing) {
            return error(res, 'Vehicle already assigned to this driver');
        }

        // If setting as primary, unset other primaries
        if (is_primary) {
            await supabaseAdmin
                .from('driver_vehicles')
                .update({ is_primary: false })
                .eq('driver_id', driver_id);
        }

        const { data: assignment, error: insertError } = await supabaseAdmin
            .from('driver_vehicles')
            .insert({
                driver_id,
                vehicle_id: id,
                is_primary: is_primary || false
            })
            .select()
            .single();

        if (insertError) {
            return error(res, 'Failed to assign vehicle');
        }

        return success(res, assignment, 'Vehicle assigned', 201);

    } catch (err) {
        console.error('Assign vehicle error:', err);
        return error(res, 'Failed to assign vehicle', 500);
    }
});

// ============================================
// DELIVERY MANAGEMENT
// ============================================

/**
 * GET /admin/deliveries
 * List all deliveries
 */
router.get('/deliveries', async (req, res) => {
    try {
        const { limit = 50, offset = 0, status } = req.query;

        let query = supabaseAdmin
            .from('deliveries')
            .select(`
        *,
        customer:customers(customer_id, name, phone),
        driver:drivers(driver_id, name, phone)
      `, { count: 'exact' })
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (status) {
            query = query.eq('status', status.toUpperCase());
        }

        const { data: deliveries, count, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch deliveries');
        }

        return success(res, { deliveries, total: count }, 'Deliveries retrieved');

    } catch (err) {
        console.error('List deliveries error:', err);
        return error(res, 'Failed to list deliveries', 500);
    }
});

// ============================================
// TRANSACTION MANAGEMENT
// ============================================

/**
 * GET /admin/transactions
 * List all payment transactions
 */
router.get('/transactions', async (req, res) => {
    try {
        const { limit = 50, offset = 0, status, payment_method } = req.query;

        let query = supabaseAdmin
            .from('payment_transactions')
            .select(`
        *,
        delivery:deliveries(
          delivery_id,
          customer:customers(name, phone),
          driver:drivers(name, phone)
        )
      `, { count: 'exact' })
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        if (status) {
            query = query.eq('status', status.toUpperCase());
        }

        if (payment_method) {
            query = query.eq('payment_method', payment_method.toUpperCase());
        }

        const { data: transactions, count, error: fetchError } = await query;

        if (fetchError) {
            return error(res, 'Failed to fetch transactions');
        }

        return success(res, { transactions, total: count }, 'Transactions retrieved');

    } catch (err) {
        console.error('List transactions error:', err);
        return error(res, 'Failed to list transactions', 500);
    }
});

module.exports = router;
