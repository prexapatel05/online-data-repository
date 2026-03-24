-- Migration: Add metadata extraction status/error columns to dataset_tables

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='dataset_tables' AND column_name='metadata_extraction_status'
    ) THEN
        ALTER TABLE dataset_tables ADD COLUMN metadata_extraction_status TEXT DEFAULT 'PENDING';
        COMMENT ON COLUMN dataset_tables.metadata_extraction_status IS 'Metadata extraction job status';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='dataset_tables' AND column_name='metadata_extraction_error'
    ) THEN
        ALTER TABLE dataset_tables ADD COLUMN metadata_extraction_error TEXT;
        COMMENT ON COLUMN dataset_tables.metadata_extraction_error IS 'Error details for metadata extraction failures';
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_dataset_tables_metadata_extraction_status
ON dataset_tables(metadata_extraction_status);

COMMIT;
