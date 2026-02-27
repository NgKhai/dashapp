/**
 * Route Proxy
 *
 * GET /routes?pickup_lat=&pickup_lng=&dropoff_lat=&dropoff_lng=
 * Proxies OSRM from the server side (public OSRM blocks Android client requests).
 * Returns an encoded polyline + distance/duration for the MapPicker preview.
 * No auth required — coordinates are not sensitive.
 */

const express = require('express');
const router = express.Router();
const { error } = require('../utils/response');

/**
 * GET /routes
 * Query params: pickup_lat, pickup_lng, dropoff_lat, dropoff_lng
 */
router.get('/', async (req, res) => {
    const { pickup_lat, pickup_lng, dropoff_lat, dropoff_lng } = req.query;

    if (!pickup_lat || !pickup_lng || !dropoff_lat || !dropoff_lng) {
        return error(res, 'pickup_lat, pickup_lng, dropoff_lat, dropoff_lng are required', 400);
    }

    const pLat = parseFloat(pickup_lat);
    const pLng = parseFloat(pickup_lng);
    const dLat = parseFloat(dropoff_lat);
    const dLng = parseFloat(dropoff_lng);

    if ([pLat, pLng, dLat, dLng].some(isNaN)) {
        return error(res, 'Coordinates must be valid numbers', 400);
    }

    try {
        const osrmUrl = `http://router.project-osrm.org/route/v1/driving/${pLng},${pLat};${dLng},${dLat}?overview=full&geometries=polyline`;
        const osrmRes = await fetch(osrmUrl, { signal: AbortSignal.timeout(8000) });

        if (!osrmRes.ok) throw new Error(`OSRM HTTP ${osrmRes.status}`);

        const osrmData = await osrmRes.json();

        if (osrmData.code !== 'Ok' || !osrmData.routes?.length) {
            throw new Error('OSRM returned no route');
        }

        const route = osrmData.routes[0];
        return res.json({
            success: true,
            data: {
                route_encoded: route.geometry,           // encoded polyline string
                distance_km: route.distance / 1000.0,    // metres → km
                duration_minutes: route.duration / 60.0  // seconds → minutes
            }
        });

    } catch (err) {
        console.warn('OSRM proxy failed, returning straight-line fallback:', err.message);

        // Haversine fallback so the app always gets a usable response
        const distanceKm = haversineKm(pLat, pLng, dLat, dLng);
        const encodedFallback = encodePolyline([[pLat, pLng], [dLat, dLng]]);

        return res.json({
            success: true,
            data: {
                route_encoded: encodedFallback,
                distance_km: distanceKm,
                duration_minutes: (distanceKm / 30.0) * 60.0
            }
        });
    }
});

// ── Util: Haversine distance ────────────────────────────────────────────────

function haversineKm(lat1, lng1, lat2, lng2) {
    const R = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function toRad(deg) { return deg * Math.PI / 180; }

// ── Util: Google Encoded Polyline Algorithm ─────────────────────────────────

function encodePolyline(points) {
    let result = '';
    let prevLat = 0, prevLng = 0;
    for (const [lat, lng] of points) {
        result += encodeValue(Math.round(lat * 1e5) - prevLat);
        result += encodeValue(Math.round(lng * 1e5) - prevLng);
        prevLat = Math.round(lat * 1e5);
        prevLng = Math.round(lng * 1e5);
    }
    return result;
}

function encodeValue(value) {
    value = value < 0 ? ~(value << 1) : value << 1;
    let encoded = '';
    while (value >= 0x20) {
        encoded += String.fromCharCode((0x20 | (value & 0x1f)) + 63);
        value >>= 5;
    }
    encoded += String.fromCharCode(value + 63);
    return encoded;
}

module.exports = router;
