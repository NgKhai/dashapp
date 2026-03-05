/**
 * Auth Routes
 * 
 * Handles authentication for both Customer and Driver apps
 * Uses Supabase Auth with phone OTP + PIN login
 */

const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const { supabase, supabaseAdmin } = require('../config/supabase');
const { success, error } = require('../utils/response');
const { verifyToken } = require('../middleware/auth');

const SALT_ROUNDS = 10;

// ============================================
// CUSTOMER AUTH ROUTES
// ============================================

/**
 * POST /auth/customer/register
 * Register a new customer with phone number
 */
router.post('/customer/register', async (req, res) => {
    try {
        const { phone, name, email } = req.body;

        // Validate required fields
        if (!phone || !name) {
            return error(res, 'Phone and name are required');
        }

        // Check if phone already exists in customers table
        const { data: existingCustomer } = await supabaseAdmin
            .from('customers')
            .select('customer_id')
            .eq('phone', phone)
            .single();

        if (existingCustomer) {
            return error(res, 'Phone number already registered as customer');
        }

        // Sign up with Supabase Auth (sends OTP)
        const { data: authData, error: authError } = await supabase.auth.signInWithOtp({
            phone,
            options: {
                shouldCreateUser: true,
                data: {
                    user_type: 'customer',
                    name
                }
            }
        });

        if (authError) {
            console.error('Supabase auth error:', authError);
            return error(res, authError.message);
        }

        // Store pending registration info (will be completed after OTP verification)
        return success(res, {
            phone,
            name,
            message: 'OTP sent to your phone. Please verify to complete registration.'
        }, 'OTP sent successfully');

    } catch (err) {
        console.error('Customer register error:', err);
        return error(res, 'Registration failed', 500);
    }
});

/**
 * POST /auth/customer/login
 * Unified Login Flow (Phone + Optional PIN)
 * 1. If PIN provided -> Verify PIN -> Login
 * 2. If PIN NOT provided:
 *    - If user has PIN -> Return { require_pin: true }
 *    - If user NO PIN -> Send OTP -> Return { require_otp: true }
 */
router.post('/customer/login', async (req, res) => {
    try {
        const { phone, pin } = req.body;

        if (!phone) {
            return error(res, 'Phone number is required');
        }

        // Check if customer exists and get necessary fields
        const { data: customer } = await supabaseAdmin
            .from('customers')
            .select('customer_id, name, email, auth_user_id, pin_hash')
            .eq('phone', phone)
            .single();

        if (!customer) {
            return error(res, 'No customer account found with this phone', 404);
        }

        // CASE 1: PIN Provided -> Try to login
        if (pin) {
            if (!customer.pin_hash) {
                return error(res, 'PIN not set. Please login with OTP first.', 400);
            }

            const isValid = await bcrypt.compare(pin, customer.pin_hash);
            if (!isValid) {
                return error(res, 'Invalid PIN', 401);
            }

            // Generate a proper Supabase-compatible JWT for PIN-authenticated user
            // Uses the Supabase JWT secret to sign a token with the original auth_user_id
            try {
                const jwt = require('jsonwebtoken');
                const jwtSecret = process.env.SUPABASE_JWT_SECRET;

                if (jwtSecret) {
                    const now = Math.floor(Date.now() / 1000);
                    const payload = {
                        sub: customer.auth_user_id, // Must match auth.users.id
                        aud: 'authenticated',
                        role: 'authenticated',
                        iat: now,
                        exp: now + 3600, // 1 hour
                        phone: phone
                    };

                    const accessToken = jwt.sign(payload, jwtSecret);
                    const refreshPayload = { ...payload, exp: now + 604800 }; // 7 days
                    const refreshToken = jwt.sign(refreshPayload, jwtSecret);

                    return success(res, {
                        customer: {
                            customer_id: customer.customer_id,
                            name: customer.name,
                            phone: phone,
                            email: customer.email
                        },
                        auth_user_id: customer.auth_user_id,
                        session: {
                            access_token: accessToken,
                            refresh_token: refreshToken,
                            expires_at: now + 3600
                        },
                        message: 'Login successful'
                    }, 'Login successful');
                }
            } catch (sessionErr) {
                console.error('JWT generation error (PIN login):', sessionErr);
            }

            // Fallback: return customer data without session
            return success(res, {
                customer: {
                    customer_id: customer.customer_id,
                    name: customer.name,
                    phone: phone,
                    email: customer.email
                },
                auth_user_id: customer.auth_user_id,
                message: 'Login successful (no session - missing JWT secret)'
            }, 'Login successful');
        }

        // CASE 2: PIN Not Provided -> Check status
        if (customer.pin_hash) {
            // User has a PIN, ask for it
            return success(res, {
                require_pin: true,
                message: 'Please enter your PIN'
            }, 'PIN required');
        } else {
            // User has NO PIN, send OTP
            const { error: authError } = await supabase.auth.signInWithOtp({
                phone
            });

            if (authError) {
                return error(res, authError.message);
            }

            return success(res, {
                require_otp: true,
                phone,
                message: 'OTP sent to your phone'
            }, 'OTP sent successfully');
        }

    } catch (err) {
        console.error('Customer login error:', err);
        return error(res, 'Login failed', 500);
    }
});

/**
 * POST /auth/customer/verify-otp
 * Verify OTP and complete registration/login
 */
router.post('/customer/verify-otp', async (req, res) => {
    try {
        const { phone, otp, name, email } = req.body;

        if (!phone || !otp) {
            return error(res, 'Phone and OTP are required');
        }

        // Verify OTP with Supabase
        const { data: authData, error: authError } = await supabase.auth.verifyOtp({
            phone,
            token: otp,
            type: 'sms'
        });

        if (authError) {
            return error(res, authError.message);
        }

        const user = authData.user;

        // Check if customer profile exists
        let { data: customer } = await supabaseAdmin
            .from('customers')
            .select('*')
            .eq('auth_user_id', user.id)
            .single();

        // If not exists, create customer profile (first time registration)
        if (!customer && name) {
            const { data: newCustomer, error: insertError } = await supabaseAdmin
                .from('customers')
                .insert({
                    auth_user_id: user.id,
                    phone,
                    name,
                    email: email || null
                })
                .select()
                .single();

            if (insertError) {
                console.error('Insert customer error:', insertError);
                return error(res, 'Failed to create customer profile');
            }

            customer = newCustomer;
        }

        return success(res, {
            user: {
                id: user.id,
                phone: user.phone
            },
            customer,
            session: {
                access_token: authData.session.access_token,
                refresh_token: authData.session.refresh_token,
                expires_at: authData.session.expires_at
            }
        }, 'Login successful');

    } catch (err) {
        console.error('Verify OTP error:', err);
        return error(res, 'Verification failed', 500);
    }
});

// ============================================
// DRIVER AUTH ROUTES
// ============================================

/**
 * POST /auth/driver/register
 * Register a new driver with phone number
 */
router.post('/driver/register', async (req, res) => {
    try {
        const { phone, name, email } = req.body;

        if (!phone || !name) {
            return error(res, 'Phone and name are required');
        }

        // Check if phone already exists in drivers table
        const { data: existingDriver } = await supabaseAdmin
            .from('drivers')
            .select('driver_id')
            .eq('phone', phone)
            .single();

        if (existingDriver) {
            return error(res, 'Phone number already registered as driver');
        }

        // Sign up with Supabase Auth (sends OTP)
        const { data: authData, error: authError } = await supabase.auth.signInWithOtp({
            phone,
            options: {
                shouldCreateUser: true,
                data: {
                    user_type: 'driver',
                    name
                }
            }
        });

        if (authError) {
            return error(res, authError.message);
        }

        return success(res, {
            phone,
            name,
            message: 'OTP sent to your phone. Please verify to complete registration.'
        }, 'OTP sent successfully');

    } catch (err) {
        console.error('Driver register error:', err);
        return error(res, 'Registration failed', 500);
    }
});

/**
 * POST /auth/driver/login
 * Unified Login Flow (Phone + Optional PIN)
 */
router.post('/driver/login', async (req, res) => {
    try {
        const { phone, pin } = req.body;

        if (!phone) {
            return error(res, 'Phone number is required');
        }

        // Check if driver exists and get necessary fields
        const { data: driver } = await supabaseAdmin
            .from('drivers')
            .select('driver_id, name, email, is_verified, is_online, auth_user_id, pin_hash')
            .eq('phone', phone)
            .single();

        if (!driver) {
            return error(res, 'No driver account found with this phone', 404);
        }

        // CASE 1: PIN Provided -> Try to login
        if (pin) {
            if (!driver.pin_hash) {
                return error(res, 'PIN not set. Please login with OTP first.', 400);
            }

            const isValid = await bcrypt.compare(pin, driver.pin_hash);
            if (!isValid) {
                return error(res, 'Invalid PIN', 401);
            }

            // Generate a proper Supabase-compatible JWT for PIN-authenticated user
            try {
                const jwt = require('jsonwebtoken');
                const jwtSecret = process.env.SUPABASE_JWT_SECRET;

                if (jwtSecret) {
                    const now = Math.floor(Date.now() / 1000);
                    const payload = {
                        sub: driver.auth_user_id,
                        aud: 'authenticated',
                        role: 'authenticated',
                        iat: now,
                        exp: now + 3600, // 1 hour
                        phone: phone
                    };

                    const accessToken = jwt.sign(payload, jwtSecret);
                    const refreshPayload = { ...payload, exp: now + 604800 }; // 7 days
                    const refreshToken = jwt.sign(refreshPayload, jwtSecret);

                    return success(res, {
                        driver: {
                            driver_id: driver.driver_id,
                            name: driver.name,
                            phone: phone,
                            email: driver.email,
                            is_verified: driver.is_verified,
                            is_online: driver.is_online
                        },
                        session: {
                            access_token: accessToken,
                            refresh_token: refreshToken,
                            expires_at: now + 3600
                        }
                    }, 'Login successful');
                }
            } catch (sessionErr) {
                console.error('JWT generation error (driver PIN login):', sessionErr);
            }

            // Fallback: return driver data without session
            return success(res, {
                driver: {
                    driver_id: driver.driver_id,
                    name: driver.name,
                    phone: phone,
                    email: driver.email,
                    is_verified: driver.is_verified,
                    is_online: driver.is_online
                },
                auth_user_id: driver.auth_user_id,
                message: 'Login successful (no session - missing JWT secret)'
            }, 'Login successful');
        }

        // CASE 2: PIN Not Provided -> Check status
        if (driver.pin_hash) {
            // User has a PIN, ask for it
            return success(res, {
                require_pin: true,
                message: 'Please enter your PIN'
            }, 'PIN required');
        } else {
            // User has NO PIN, send OTP
            const { error: authError } = await supabase.auth.signInWithOtp({
                phone
            });

            if (authError) {
                return error(res, authError.message);
            }

            return success(res, {
                require_otp: true,
                phone,
                message: 'OTP sent to your phone'
            }, 'OTP sent successfully');
        }

    } catch (err) {
        console.error('Driver login error:', err);
        return error(res, 'Login failed', 500);
    }
});

/**
 * POST /auth/driver/verify-otp
 * Verify OTP and complete registration/login
 */
router.post('/driver/verify-otp', async (req, res) => {
    try {
        const { phone, otp, name, email } = req.body;

        if (!phone || !otp) {
            return error(res, 'Phone and OTP are required');
        }

        // Verify OTP with Supabase
        const { data: authData, error: authError } = await supabase.auth.verifyOtp({
            phone,
            token: otp,
            type: 'sms'
        });

        if (authError) {
            return error(res, authError.message);
        }

        const user = authData.user;

        // Check if driver profile exists
        let { data: driver } = await supabaseAdmin
            .from('drivers')
            .select('*')
            .eq('auth_user_id', user.id)
            .single();

        // If not exists, create driver profile (first time registration)
        if (!driver && name) {
            const { data: newDriver, error: insertError } = await supabaseAdmin
                .from('drivers')
                .insert({
                    auth_user_id: user.id,
                    phone,
                    name,
                    email: email || null,
                    is_verified: false,  // Drivers need admin verification
                    is_online: false
                })
                .select()
                .single();

            if (insertError) {
                console.error('Insert driver error:', insertError);
                return error(res, 'Failed to create driver profile');
            }

            driver = newDriver;
        }

        return success(res, {
            user: {
                id: user.id,
                phone: user.phone
            },
            driver,
            session: {
                access_token: authData.session.access_token,
                refresh_token: authData.session.refresh_token,
                expires_at: authData.session.expires_at
            }
        }, 'Login successful');

    } catch (err) {
        console.error('Verify OTP error:', err);
        return error(res, 'Verification failed', 500);
    }
});

// ============================================
// COMMON AUTH ROUTES
// ============================================

/**
 * POST /auth/logout
 * Logout current user
 */
router.post('/logout', verifyToken, async (req, res) => {
    try {
        const { error: logoutError } = await supabase.auth.signOut();

        if (logoutError) {
            return error(res, logoutError.message);
        }

        return success(res, null, 'Logged out successfully');

    } catch (err) {
        console.error('Logout error:', err);
        return error(res, 'Logout failed', 500);
    }
});

/**
 * GET /auth/me
 * Get current user profile
 */
router.get('/me', verifyToken, async (req, res) => {
    try {
        const userId = req.user.id;

        // Try to find as customer
        const { data: customer } = await supabaseAdmin
            .from('customers')
            .select('*')
            .eq('auth_user_id', userId)
            .single();

        if (customer) {
            return success(res, {
                type: 'customer',
                profile: customer
            });
        }

        // Try to find as driver
        const { data: driver } = await supabaseAdmin
            .from('drivers')
            .select('*')
            .eq('auth_user_id', userId)
            .single();

        if (driver) {
            return success(res, {
                type: 'driver',
                profile: driver
            });
        }

        // Try to find as admin
        const { data: admin } = await supabaseAdmin
            .from('admins')
            .select('*')
            .eq('auth_user_id', userId)
            .single();

        if (admin) {
            return success(res, {
                type: 'admin',
                profile: admin
            });
        }

        return error(res, 'User profile not found', 404);

    } catch (err) {
        console.error('Get me error:', err);
        return error(res, 'Failed to get user profile', 500);
    }
});

/**
 * POST /auth/refresh
 * Refresh access token
 */
router.post('/refresh', async (req, res) => {
    try {
        const { refresh_token } = req.body;

        if (!refresh_token) {
            return error(res, 'Refresh token is required');
        }

        // Try 1: Supabase-issued refresh token (from OTP login)
        const { data, error: refreshError } = await supabase.auth.refreshSession({
            refresh_token
        });

        if (!refreshError && data?.session) {
            return success(res, {
                access_token: data.session.access_token,
                refresh_token: data.session.refresh_token,
                expires_at: data.session.expires_at
            }, 'Token refreshed');
        }

        // Try 2: Custom JWT refresh token (from PIN login)
        const jwtSecret = process.env.SUPABASE_JWT_SECRET;
        if (jwtSecret) {
            try {
                const jwt = require('jsonwebtoken');
                const decoded = jwt.verify(refresh_token, jwtSecret);

                // Valid custom refresh token — issue new token pair
                if (decoded.sub && decoded.role === 'authenticated') {
                    const now = Math.floor(Date.now() / 1000);

                    const newAccessPayload = {
                        sub: decoded.sub,
                        aud: 'authenticated',
                        role: 'authenticated',
                        iat: now,
                        exp: now + 3600, // 1 hour
                        phone: decoded.phone
                    };

                    const newRefreshPayload = {
                        ...newAccessPayload,
                        exp: now + 604800 // 7 days
                    };

                    const newAccessToken = jwt.sign(newAccessPayload, jwtSecret);
                    const newRefreshToken = jwt.sign(newRefreshPayload, jwtSecret);

                    return success(res, {
                        access_token: newAccessToken,
                        refresh_token: newRefreshToken,
                        expires_at: now + 3600
                    }, 'Token refreshed');
                }
            } catch (jwtErr) {
                console.error('Custom JWT refresh failed:', jwtErr.message);
            }
        }

        return error(res, 'Refresh token is not valid', 401);

    } catch (err) {
        console.error('Refresh error:', err);
        return error(res, 'Token refresh failed', 500);
    }
});

// ============================================
// PIN AUTH ROUTES
// ============================================

/**
 * POST /auth/set-pin
 * Set or update 6-digit PIN (requires active session)
 * Can also update name on first setup
 */
router.post('/set-pin', verifyToken, async (req, res) => {
    try {
        const { pin, user_type, name } = req.body;

        // Validate PIN format (must be exactly 6 digits)
        if (!pin || !/^\d{6}$/.test(pin)) {
            return error(res, 'PIN must be exactly 6 digits');
        }

        if (!user_type || !['customer', 'driver'].includes(user_type)) {
            return error(res, 'user_type must be "customer" or "driver"');
        }

        // Hash the PIN
        const pin_hash = await bcrypt.hash(pin, SALT_ROUNDS);

        // Build update object
        const updateData = { pin_hash };

        // Include name if provided (for first-time setup)
        if (name && name.trim()) {
            updateData.name = name.trim();
        }

        // Update the appropriate table
        const tableName = user_type === 'customer' ? 'customers' : 'drivers';

        const { data: updatedUser, error: updateError } = await supabaseAdmin
            .from(tableName)
            .update(updateData)
            .eq('auth_user_id', req.user.id)
            .select('name')
            .single();

        if (updateError) {
            console.error('Set PIN error:', updateError);
            return error(res, 'Failed to set PIN');
        }

        return success(res, {
            name: updatedUser?.name
        }, 'PIN set successfully');

    } catch (err) {
        console.error('Set PIN error:', err);
        return error(res, 'Failed to set PIN', 500);
    }
});

// check-pin-status and login-pin routes removed - logic merged into login endpoints

module.exports = router;
