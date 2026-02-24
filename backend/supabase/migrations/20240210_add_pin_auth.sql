-- Migration: Add PIN authentication support

-- Add pin_hash column to customers table
ALTER TABLE customers ADD COLUMN pin_hash TEXT;

-- Add pin_hash column to drivers table
ALTER TABLE drivers ADD COLUMN pin_hash TEXT;

-- Comment on columns
COMMENT ON COLUMN customers.pin_hash IS 'Bcrypt hashed 6-digit PIN for quick login';
COMMENT ON COLUMN drivers.pin_hash IS 'Bcrypt hashed 6-digit PIN for quick login';
