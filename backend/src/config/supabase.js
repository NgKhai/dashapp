/**
 * Supabase Client Configuration
 * 
 * This file creates two Supabase clients:
 * 1. supabase - Uses anon key (for client-side auth operations)
 * 2. supabaseAdmin - Uses service key (for admin operations, bypasses RLS)
 */

const { createClient } = require('@supabase/supabase-js');

// Load environment variables
require('dotenv').config();

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY;

// Validate required environment variables
if (!supabaseUrl || !supabaseAnonKey) {
  console.error('Missing Supabase environment variables!');
  console.error('Make sure SUPABASE_URL and SUPABASE_ANON_KEY are set in .env');
}

// Client for normal operations (respects RLS)
const supabase = createClient(supabaseUrl, supabaseAnonKey);

// Admin client for privileged operations (bypasses RLS)
const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false
  }
});

module.exports = { supabase, supabaseAdmin };
