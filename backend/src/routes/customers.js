/**
 * Customer Routes
 * 
 * Endpoints for customer profile and address management
 */

const express = require('express');
const router = express.Router();
const { supabaseAdmin } = require('../config/supabase');
const { success, error, notFound } = require('../utils/response');
const { verifyToken, requireCustomer } = require('../middleware/auth');

// All routes require authentication as customer
router.use(verifyToken, requireCustomer);

// ============================================
// PROFILE ROUTES
// ============================================

/**
 * GET /customers/profile
 * Get current customer's profile
 */
router.get('/profile', async (req, res) => {
    try {
        return success(res, req.customer, 'Profile retrieved');
    } catch (err) {
        console.error('Get profile error:', err);
        return error(res, 'Failed to get profile', 500);
    }
});

/**
 * PUT /customers/profile
 * Update current customer's profile
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

        const { data: customer, error: updateError } = await supabaseAdmin
            .from('customers')
            .update(updateData)
            .eq('customer_id', req.customer.customer_id)
            .select()
            .single();

        if (updateError) {
            console.error('Update profile error:', updateError);
            return error(res, 'Failed to update profile');
        }

        return success(res, customer, 'Profile updated');

    } catch (err) {
        console.error('Update profile error:', err);
        return error(res, 'Failed to update profile', 500);
    }
});

// ============================================
// ADDRESS ROUTES
// ============================================

/**
 * GET /customers/addresses
 * Get all saved addresses for customer
 */
router.get('/addresses', async (req, res) => {
    try {
        const { data: addresses, error: fetchError } = await supabaseAdmin
            .from('customer_addresses')
            .select('*')
            .eq('customer_id', req.customer.customer_id)
            .order('is_default', { ascending: false })
            .order('created_at', { ascending: false });

        if (fetchError) {
            return error(res, 'Failed to fetch addresses');
        }

        return success(res, addresses, 'Addresses retrieved');

    } catch (err) {
        console.error('Get addresses error:', err);
        return error(res, 'Failed to get addresses', 500);
    }
});

/**
 * POST /customers/addresses
 * Add a new address
 */
router.post('/addresses', async (req, res) => {
    try {
        const { label, address, lat, lng, is_default } = req.body;

        // Validate required fields
        if (!label || !address || lat === undefined || lng === undefined) {
            return error(res, 'Label, address, lat, and lng are required');
        }

        // If setting as default, unset other defaults first
        if (is_default) {
            await supabaseAdmin
                .from('customer_addresses')
                .update({ is_default: false })
                .eq('customer_id', req.customer.customer_id);
        }

        const { data: newAddress, error: insertError } = await supabaseAdmin
            .from('customer_addresses')
            .insert({
                customer_id: req.customer.customer_id,
                label,
                address,
                lat,
                lng,
                is_default: is_default || false
            })
            .select()
            .single();

        if (insertError) {
            console.error('Insert address error:', insertError);
            return error(res, 'Failed to add address');
        }

        return success(res, newAddress, 'Address added', 201);

    } catch (err) {
        console.error('Add address error:', err);
        return error(res, 'Failed to add address', 500);
    }
});

/**
 * PUT /customers/addresses/:id
 * Update an address
 */
router.put('/addresses/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { label, address, lat, lng, is_default } = req.body;

        // Verify address belongs to customer
        const { data: existingAddress } = await supabaseAdmin
            .from('customer_addresses')
            .select('address_id')
            .eq('address_id', id)
            .eq('customer_id', req.customer.customer_id)
            .single();

        if (!existingAddress) {
            return notFound(res, 'Address not found');
        }

        // If setting as default, unset other defaults first
        if (is_default) {
            await supabaseAdmin
                .from('customer_addresses')
                .update({ is_default: false })
                .eq('customer_id', req.customer.customer_id);
        }

        const updateData = {};
        if (label) updateData.label = label;
        if (address) updateData.address = address;
        if (lat !== undefined) updateData.lat = lat;
        if (lng !== undefined) updateData.lng = lng;
        if (is_default !== undefined) updateData.is_default = is_default;

        const { data: updatedAddress, error: updateError } = await supabaseAdmin
            .from('customer_addresses')
            .update(updateData)
            .eq('address_id', id)
            .select()
            .single();

        if (updateError) {
            return error(res, 'Failed to update address');
        }

        return success(res, updatedAddress, 'Address updated');

    } catch (err) {
        console.error('Update address error:', err);
        return error(res, 'Failed to update address', 500);
    }
});

/**
 * DELETE /customers/addresses/:id
 * Delete an address
 */
router.delete('/addresses/:id', async (req, res) => {
    try {
        const { id } = req.params;

        // Verify address belongs to customer
        const { data: existingAddress } = await supabaseAdmin
            .from('customer_addresses')
            .select('address_id')
            .eq('address_id', id)
            .eq('customer_id', req.customer.customer_id)
            .single();

        if (!existingAddress) {
            return notFound(res, 'Address not found');
        }

        const { error: deleteError } = await supabaseAdmin
            .from('customer_addresses')
            .delete()
            .eq('address_id', id);

        if (deleteError) {
            return error(res, 'Failed to delete address');
        }

        return success(res, null, 'Address deleted');

    } catch (err) {
        console.error('Delete address error:', err);
        return error(res, 'Failed to delete address', 500);
    }
});

// ============================================
// DELIVERY HISTORY
// ============================================

/**
 * GET /customers/deliveries
 * Get customer's delivery history
 */
router.get('/deliveries', async (req, res) => {
    try {
        const { status, limit = 20, offset = 0 } = req.query;

        let query = supabaseAdmin
            .from('deliveries')
            .select(`
        *,
        driver:drivers(driver_id, name, phone, avatar_url, rating)
      `)
            .eq('customer_id', req.customer.customer_id)
            .order('created_at', { ascending: false })
            .range(offset, offset + limit - 1);

        // Filter by status if provided
        if (status) {
            query = query.eq('status', status.toUpperCase());
        }

        const { data: deliveries, error: fetchError } = await query;

        if (fetchError) {
            console.error('Fetch deliveries error:', fetchError);
            return error(res, 'Failed to fetch deliveries');
        }

        return success(res, deliveries, 'Deliveries retrieved');

    } catch (err) {
        console.error('Get deliveries error:', err);
        return error(res, 'Failed to get deliveries', 500);
    }
});

module.exports = router;
