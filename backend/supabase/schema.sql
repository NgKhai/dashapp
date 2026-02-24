-- ============================================
-- DashApp Delivery Database Schema for Supabase
-- ============================================
-- Run this in Supabase SQL Editor to create all tables
-- ============================================

-- Enable UUID extension (if not already enabled)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- 1. CUSTOMERS TABLE
-- ============================================
CREATE TABLE customers (
  customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  phone TEXT NOT NULL UNIQUE,
  email TEXT UNIQUE,
  avatar_url TEXT,
  auth_user_id UUID REFERENCES auth.users(id),
  pin_hash TEXT, -- Bcrypt hashed 6-digit PIN for quick login
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON COLUMN customers.pin_hash IS 'Bcrypt hashed 6-digit PIN for quick login';

-- Index for faster lookups
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_email ON customers(email);

-- ============================================
-- 2. CUSTOMER ADDRESSES TABLE (Saved addresses)
-- ============================================
CREATE TABLE customer_addresses (
  address_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  customer_id UUID NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
  label TEXT NOT NULL, -- 'Home', 'Work', 'Other'
  address TEXT NOT NULL,
  lat FLOAT NOT NULL,
  lng FLOAT NOT NULL,
  is_default BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses(customer_id);

-- ============================================
-- 3. DRIVERS TABLE
-- ============================================
CREATE TABLE drivers (
  driver_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  phone TEXT NOT NULL UNIQUE,
  email TEXT UNIQUE,
  avatar_url TEXT,
  auth_user_id UUID REFERENCES auth.users(id),
  pin_hash TEXT, -- Bcrypt hashed 6-digit PIN for quick login
  is_online BOOLEAN DEFAULT false,
  is_verified BOOLEAN DEFAULT false,
  is_active BOOLEAN DEFAULT true,
  rating FLOAT DEFAULT 0.0 CHECK (rating >= 0 AND rating <= 5),
  total_deliveries INT DEFAULT 0,
  total_ratings INT DEFAULT 0,
  current_lat FLOAT,
  current_lng FLOAT,
  last_location_update TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON COLUMN drivers.pin_hash IS 'Bcrypt hashed 6-digit PIN for quick login';

-- Indexes for driver lookups
CREATE INDEX idx_drivers_phone ON drivers(phone);
CREATE INDEX idx_drivers_is_online ON drivers(is_online);
CREATE INDEX idx_drivers_location ON drivers(current_lat, current_lng);

-- ============================================
-- 4. VEHICLES TABLE
-- ============================================
CREATE TABLE vehicles (
  vehicle_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  type TEXT NOT NULL CHECK (type IN ('MOTORCYCLE', 'CAR', 'VAN', 'TRUCK')),
  plate_number TEXT NOT NULL UNIQUE,
  model_name TEXT NOT NULL,
  color TEXT NOT NULL,
  capacity_kg FLOAT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_vehicles_plate ON vehicles(plate_number);
CREATE INDEX idx_vehicles_type ON vehicles(type);

-- ============================================
-- 5. DRIVER-VEHICLE JUNCTION TABLE (Many-to-Many)
-- ============================================
CREATE TABLE driver_vehicles (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  driver_id UUID NOT NULL REFERENCES drivers(driver_id) ON DELETE CASCADE,
  vehicle_id UUID NOT NULL REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
  is_primary BOOLEAN DEFAULT false, -- Currently active vehicle for the driver
  assigned_at TIMESTAMPTZ DEFAULT NOW(),
  
  -- Ensure unique pairing
  UNIQUE(driver_id, vehicle_id)
);

CREATE INDEX idx_driver_vehicles_driver ON driver_vehicles(driver_id);
CREATE INDEX idx_driver_vehicles_vehicle ON driver_vehicles(vehicle_id);

-- ============================================
-- 6. DELIVERIES TABLE
-- ============================================
CREATE TABLE deliveries (
  delivery_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  customer_id UUID NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT,
  driver_id UUID REFERENCES drivers(driver_id) ON DELETE SET NULL,
  vehicle_id UUID REFERENCES vehicles(vehicle_id) ON DELETE SET NULL,
  
  -- Status tracking
  status TEXT DEFAULT 'PENDING' CHECK (status IN (
    'PENDING',      -- Waiting for driver
    'ACCEPTED',     -- Driver accepted
    'PICKED_UP',    -- Package picked up
    'DELIVERING',   -- On the way
    'COMPLETED',    -- Delivered successfully
    'CANCELLED'     -- Cancelled
  )),
  
  -- Pickup location
  pickup_lat FLOAT NOT NULL,
  pickup_lng FLOAT NOT NULL,
  pickup_address TEXT NOT NULL,
  
  -- Drop-off location
  drop_off_lat FLOAT NOT NULL,
  drop_off_lng FLOAT NOT NULL,
  drop_off_address TEXT NOT NULL,
  
  -- Pricing & distance
  total_price FLOAT NOT NULL,
  distance_km FLOAT NOT NULL,
  estimated_duration_min INT,
  
  -- Special instructions
  notes TEXT,

  -- Item details (AI Analysis)
  items_photo_url TEXT, -- URL of the photo taken by customer
  items JSONB DEFAULT '[]'::jsonb, -- JSON list of items detected by Mobile AI (ML Kit)
  requires_loading_help BOOLEAN DEFAULT false, -- Whether customer needs driver help with loading
  
  -- Timestamps for each status
  accepted_at TIMESTAMPTZ,
  picked_up_at TIMESTAMPTZ,
  delivered_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  
  -- Cancellation details
  cancelled_by TEXT CHECK (cancelled_by IN ('CUSTOMER', 'DRIVER', 'SYSTEM')),
  cancellation_reason TEXT,
  
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON COLUMN deliveries.items_photo_url IS 'URL of the photo taken by customer';
COMMENT ON COLUMN deliveries.items IS 'JSON list of items detected by Mobile AI (ML Kit)';
COMMENT ON COLUMN deliveries.requires_loading_help IS 'Whether customer needs driver help with loading';

-- Indexes for delivery lookups
CREATE INDEX idx_deliveries_customer ON deliveries(customer_id);
CREATE INDEX idx_deliveries_driver ON deliveries(driver_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_deliveries_created ON deliveries(created_at DESC);

-- ============================================
-- 7. DELIVERY RATINGS TABLE
-- ============================================
CREATE TABLE delivery_ratings (
  rating_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  delivery_id UUID NOT NULL REFERENCES deliveries(delivery_id) ON DELETE CASCADE,
  
  -- Customer rates driver
  customer_rating INT CHECK (customer_rating >= 1 AND customer_rating <= 5),
  customer_review TEXT,
  
  -- Driver rates customer (optional)
  driver_rating INT CHECK (driver_rating >= 1 AND driver_rating <= 5),
  driver_review TEXT,
  
  created_at TIMESTAMPTZ DEFAULT NOW(),
  
  -- One rating per delivery
  UNIQUE(delivery_id)
);

CREATE INDEX idx_ratings_delivery ON delivery_ratings(delivery_id);

-- ============================================
-- 8. DRIVER LOCATION HISTORY TABLE (Real-time tracking)
-- ============================================
CREATE TABLE driver_locations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  driver_id UUID NOT NULL REFERENCES drivers(driver_id) ON DELETE CASCADE,
  delivery_id UUID REFERENCES deliveries(delivery_id) ON DELETE SET NULL, -- Optional: track during delivery
  lat FLOAT NOT NULL,
  lng FLOAT NOT NULL,
  recorded_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for location history queries
CREATE INDEX idx_driver_locations_driver ON driver_locations(driver_id);
CREATE INDEX idx_driver_locations_delivery ON driver_locations(delivery_id);
CREATE INDEX idx_driver_locations_time ON driver_locations(recorded_at DESC);

-- ============================================
-- 9. PAYMENT TRANSACTIONS TABLE
-- ============================================
CREATE TABLE payment_transactions (
  transaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  delivery_id UUID NOT NULL REFERENCES deliveries(delivery_id) ON DELETE RESTRICT,
  
  amount FLOAT NOT NULL,
  payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'EWALLET', 'BANK_TRANSFER')),
  status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
  
  -- Payment provider reference (if using external provider)
  provider_ref TEXT,
  
  paid_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_payments_delivery ON payment_transactions(delivery_id);
CREATE INDEX idx_payments_status ON payment_transactions(status);

-- ============================================
-- 10. UPDATED_AT TRIGGER FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER update_customers_updated_at
  BEFORE UPDATE ON customers
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_drivers_updated_at
  BEFORE UPDATE ON drivers
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vehicles_updated_at
  BEFORE UPDATE ON vehicles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_deliveries_updated_at
  BEFORE UPDATE ON deliveries
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at
  BEFORE UPDATE ON payment_transactions
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 11. UPDATE DRIVER RATING FUNCTION
-- ============================================
-- Automatically update driver's average rating when a new rating is added
CREATE OR REPLACE FUNCTION update_driver_rating()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE drivers
  SET 
    rating = (
      SELECT COALESCE(AVG(dr.customer_rating), 0)
      FROM delivery_ratings dr
      JOIN deliveries d ON dr.delivery_id = d.delivery_id
      WHERE d.driver_id = (SELECT driver_id FROM deliveries WHERE delivery_id = NEW.delivery_id)
      AND dr.customer_rating IS NOT NULL
    ),
    total_ratings = (
      SELECT COUNT(*)
      FROM delivery_ratings dr
      JOIN deliveries d ON dr.delivery_id = d.delivery_id
      WHERE d.driver_id = (SELECT driver_id FROM deliveries WHERE delivery_id = NEW.delivery_id)
      AND dr.customer_rating IS NOT NULL
    )
  WHERE driver_id = (SELECT driver_id FROM deliveries WHERE delivery_id = NEW.delivery_id);
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_driver_rating
  AFTER INSERT OR UPDATE ON delivery_ratings
  FOR EACH ROW EXECUTE FUNCTION update_driver_rating();

-- ============================================
-- 12. INCREMENT DRIVER DELIVERY COUNT
-- ============================================
CREATE OR REPLACE FUNCTION increment_driver_deliveries()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
    UPDATE drivers
    SET total_deliveries = total_deliveries + 1
    WHERE driver_id = NEW.driver_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_deliveries
  AFTER UPDATE ON deliveries
  FOR EACH ROW EXECUTE FUNCTION increment_driver_deliveries();

-- ============================================
-- ROW LEVEL SECURITY (RLS) - Basic Setup
-- ============================================
-- Enable RLS on all tables
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE drivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE driver_vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE deliveries ENABLE ROW LEVEL SECURITY;
ALTER TABLE delivery_ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE driver_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;

-- Note: You'll need to add specific RLS policies based on your auth setup
-- Example policy (customize based on your auth):
-- CREATE POLICY "Customers can view own data" ON customers
--   FOR SELECT USING (auth.uid()::text = customer_id::text);

-- ============================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================
-- Uncomment to insert sample data

-- INSERT INTO customers (name, phone, email) VALUES
-- ('John Doe', '+84123456789', 'john@example.com'),
-- ('Jane Smith', '+84987654321', 'jane@example.com');

-- INSERT INTO drivers (name, phone, email, is_verified) VALUES
-- ('Driver One', '+84111222333', 'driver1@example.com', true),
-- ('Driver Two', '+84444555666', 'driver2@example.com', true);

-- INSERT INTO vehicles (type, plate_number, model_name, color) VALUES
-- ('MOTORCYCLE', '59A-12345', 'Honda Wave', 'Black'),
-- ('CAR', '51A-67890', 'Toyota Vios', 'White');
