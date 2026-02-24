-- Migration: Add AI Analysis & Loading Help fields to deliveries table

-- 1. Add new columns
ALTER TABLE deliveries 
ADD COLUMN items_photo_url TEXT,
ADD COLUMN items JSONB DEFAULT '[]'::jsonb, -- Store list of detected items (name, confidence, etc.)
ADD COLUMN requires_loading_help BOOLEAN DEFAULT false;

-- 2. Comment on columns
COMMENT ON COLUMN deliveries.items_photo_url IS 'URL of the photo taken by customer';
COMMENT ON COLUMN deliveries.items IS 'JSON list of items detected by Mobile AI (ML Kit)';
COMMENT ON COLUMN deliveries.requires_loading_help IS 'Whether customer needs driver help with loading';
