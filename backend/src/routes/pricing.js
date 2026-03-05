/**
 * Pricing Routes
 * 
 * Public endpoint to fetch delivery pricing configuration per vehicle type.
 */

const express = require('express');
const router = express.Router();
const { supabaseAdmin } = require('../config/supabase');
const { success, error } = require('../utils/response');

// Default pricing fallback (used if DB fetch fails)
const DEFAULT_PRICING = [
    { vehicle_type: 'MOTORCYCLE', base_fare: 15000, per_km: 4000, loading_help_fee: 30000 },
    { vehicle_type: 'CAR', base_fare: 25000, per_km: 6000, loading_help_fee: 50000 },
    { vehicle_type: 'VAN', base_fare: 35000, per_km: 8000, loading_help_fee: 60000 },
    { vehicle_type: 'TRUCK', base_fare: 50000, per_km: 12000, loading_help_fee: 80000 }
];

/**
 * GET /pricing
 * Returns all active pricing configs (no auth required)
 * Optional query: ?type=MOTORCYCLE to filter by vehicle type
 */
router.get('/', async (req, res) => {
    try {
        let query = supabaseAdmin
            .from('pricing_config')
            .select('vehicle_type, base_fare, per_km, loading_help_fee')
            .eq('is_active', true)
            .order('vehicle_type');

        const { type } = req.query;
        if (type) {
            query = query.eq('vehicle_type', type.toUpperCase());
        }

        const { data: pricing, error: fetchError } = await query;

        if (fetchError || !pricing || pricing.length === 0) {
            const fallback = type
                ? DEFAULT_PRICING.filter(p => p.vehicle_type === type.toUpperCase())
                : DEFAULT_PRICING;
            return success(res, fallback, 'Pricing config retrieved (default)');
        }

        return success(res, pricing, 'Pricing config retrieved');

    } catch (err) {
        console.error('Get pricing error:', err);
        return success(res, DEFAULT_PRICING, 'Pricing config retrieved (fallback)');
    }
});

module.exports = router;
