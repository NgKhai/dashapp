/**
 * DashApp Backend API
 * 
 * Main entry point for the Express application
 * Designed for Vercel serverless deployment
 */

const express = require('express');
const cors = require('cors');
require('dotenv').config();

// Initialize Express app
const app = express();

// ============================================
// MIDDLEWARE
// ============================================

// Enable CORS for all origins (configure for production)
app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'x-vercel-protection-bypass']
}));

// Parse JSON bodies
app.use(express.json());

// Request logging (simple)
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// ============================================
// ROUTES
// ============================================

// Import route modules
// Import route modules
const authRoutes = require('../src/routes/auth');
const customerRoutes = require('../src/routes/customers');
const driverRoutes = require('../src/routes/drivers');
const deliveryRoutes = require('../src/routes/deliveries');
const adminRoutes = require('../src/routes/admin');
const routeRoutes = require('../src/routes/routes');
const pricingRoutes = require('../src/routes/pricing');

// Health check endpoint
app.get('/', (req, res) => {
    res.json({
        success: true,
        message: 'DashApp API is running',
        version: '1.0.0',
        endpoints: {
            auth: '/auth',
            customers: '/customers',
            drivers: '/drivers',
            deliveries: '/deliveries',
            admin: '/admin',
            pricing: '/pricing'
        }
    });
});

app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Mount routes
app.use('/auth', authRoutes);
app.use('/customers', customerRoutes);
app.use('/drivers', driverRoutes);
app.use('/deliveries', deliveryRoutes);
app.use('/admin', adminRoutes);
app.use('/routes', routeRoutes);   // OSRM proxy for MapPicker road route preview
app.use('/pricing', pricingRoutes); // Public pricing config endpoint

// ============================================
// ERROR HANDLING
// ============================================

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        message: 'Endpoint not found'
    });
});

// Global error handler
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({
        success: false,
        message: 'Internal server error'
    });
});

// ============================================
// SERVER START (for local development)
// ============================================

const PORT = process.env.PORT || 3000;

// Only start server if not in Vercel environment
if (process.env.NODE_ENV !== 'production') {
    app.listen(PORT, () => {
        console.log(`
╔════════════════════════════════════════════╗
║         DashApp Backend API                ║
╠════════════════════════════════════════════╣
║  Server running on http://localhost:${PORT}   ║
║                                            ║
║  Endpoints:                                ║
║  - GET  /              Health check        ║
║  - POST /auth/*        Authentication      ║
║  - *    /customers/*   Customer APIs       ║
║  - *    /drivers/*     Driver APIs         ║
║  - *    /deliveries/*  Delivery APIs       ║
║  - *    /admin/*       Admin APIs          ║
╚════════════════════════════════════════════╝
    `);
    });
}

// Export for Vercel
module.exports = app;
