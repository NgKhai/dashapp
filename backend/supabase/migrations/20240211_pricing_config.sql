-- ============================================
-- PRICING CONFIG TABLE (per vehicle type)
-- ============================================
-- Stores delivery pricing parameters per vehicle type
-- so they can be updated without redeploying the app or backend.

CREATE TABLE pricing_config (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  vehicle_type TEXT NOT NULL CHECK (vehicle_type IN ('MOTORCYCLE', 'CAR', 'VAN', 'TRUCK')),
  base_fare INT NOT NULL,                        -- Base fare in VND
  per_km INT NOT NULL,                           -- Per-km charge in VND
  loading_help_fee INT NOT NULL DEFAULT 50000,   -- Loading help surcharge in VND
  is_active BOOLEAN DEFAULT true,
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  -- One active config per vehicle type
  UNIQUE(vehicle_type, is_active)
);

-- Seed with sample pricing per vehicle type
INSERT INTO pricing_config (vehicle_type, base_fare, per_km, loading_help_fee, is_active) VALUES
  ('MOTORCYCLE', 15000, 4000, 30000, true),
  ('CAR',        25000, 6000, 50000, true),
  ('VAN',        35000, 8000, 60000, true),
  ('TRUCK',      50000, 12000, 80000, true);

-- Apply updated_at trigger
CREATE TRIGGER update_pricing_config_updated_at
  BEFORE UPDATE ON pricing_config
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Enable RLS
ALTER TABLE pricing_config ENABLE ROW LEVEL SECURITY;

-- Allow anyone to read pricing (public info)
CREATE POLICY "Anyone can read active pricing" ON pricing_config
  FOR SELECT USING (is_active = true);
