-- Add file_identifier column to files table
ALTER TABLE files ADD COLUMN file_identifier TEXT;

-- Create index for file_identifier to improve query performance
CREATE INDEX IF NOT EXISTS idx_files_file_identifier ON files(file_identifier);