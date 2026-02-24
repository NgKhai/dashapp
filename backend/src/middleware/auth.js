/**
 * Authentication Middleware
 * 
 * Verifies JWT tokens from Supabase Auth and attaches user info to request
 * Supports both native Supabase tokens and custom-signed JWTs (PIN login)
 */

const { supabase, supabaseAdmin } = require('../config/supabase');
const { unauthorized, forbidden } = require('../utils/response');

/**
 * Verify JWT token from Authorization header
 * Attaches user info to req.user if valid
 */
const verifyToken = async (req, res, next) => {
    try {
        // Get token from header
        const authHeader = req.headers.authorization;

        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return unauthorized(res, 'No token provided');
        }

        const token = authHeader.split(' ')[1];

        // Try 1: Verify with Supabase (native OTP-based tokens)
        const { data: { user }, error } = await supabase.auth.getUser(token);

        if (!error && user) {
            req.user = user;
            req.token = token;
            return next();
        }

        // Try 2: Verify custom JWT (PIN login tokens)
        const jwtSecret = process.env.SUPABASE_JWT_SECRET;
        if (jwtSecret) {
            try {
                const jwt = require('jsonwebtoken');
                const decoded = jwt.verify(token, jwtSecret);

                if (decoded.sub && decoded.role === 'authenticated') {
                    // Build a minimal user object matching Supabase's format
                    req.user = {
                        id: decoded.sub,
                        phone: decoded.phone,
                        role: 'authenticated'
                    };
                    req.token = token;
                    return next();
                }
            } catch (jwtErr) {
                console.error('Custom JWT verification failed:', jwtErr.message);
            }
        }

        return unauthorized(res, 'Invalid or expired token');
    } catch (err) {
        console.error('Auth middleware error:', err);
        return unauthorized(res, 'Authentication failed');
    }
};

/**
 * Require user to be a customer
 * Must be used after verifyToken
 */
const requireCustomer = async (req, res, next) => {
    try {
        if (!req.user) {
            return unauthorized(res, 'Authentication required');
        }

        // Check if user exists in customers table
        const { data: customer, error } = await supabaseAdmin
            .from('customers')
            .select('*')
            .eq('auth_user_id', req.user.id)
            .single();

        if (error || !customer) {
            return forbidden(res, 'Customer account required');
        }

        // Attach customer profile to request
        req.customer = customer;

        next();
    } catch (err) {
        console.error('requireCustomer error:', err);
        return forbidden(res, 'Customer verification failed');
    }
};

/**
 * Require user to be a driver
 * Must be used after verifyToken
 */
const requireDriver = async (req, res, next) => {
    try {
        if (!req.user) {
            return unauthorized(res, 'Authentication required');
        }

        // Check if user exists in drivers table
        const { data: driver, error } = await supabaseAdmin
            .from('drivers')
            .select('*')
            .eq('auth_user_id', req.user.id)
            .single();

        if (error || !driver) {
            return forbidden(res, 'Driver account required');
        }

        // Attach driver profile to request
        req.driver = driver;

        next();
    } catch (err) {
        console.error('requireDriver error:', err);
        return forbidden(res, 'Driver verification failed');
    }
};

/**
 * Require user to be an admin
 * Must be used after verifyToken
 */
const requireAdmin = async (req, res, next) => {
    try {
        if (!req.user) {
            return unauthorized(res, 'Authentication required');
        }

        // Check if user exists in admins table
        const { data: admin, error } = await supabaseAdmin
            .from('admins')
            .select('*')
            .eq('auth_user_id', req.user.id)
            .single();

        if (error || !admin) {
            return forbidden(res, 'Admin access required');
        }

        // Check if admin is active
        if (!admin.is_active) {
            return forbidden(res, 'Admin account is disabled');
        }

        // Attach admin profile to request
        req.admin = admin;

        next();
    } catch (err) {
        console.error('requireAdmin error:', err);
        return forbidden(res, 'Admin verification failed');
    }
};

module.exports = {
    verifyToken,
    requireCustomer,
    requireDriver,
    requireAdmin
};
