-- BE-09 (ADR-09, NFR-04): marks rows whose address is stored encrypted. The subscriber type decides this when the
-- row is written (secret addresses such as Slack webhook URLs); reading uses the flag, never the column content.
ALTER TABLE subscriber ADD COLUMN address_encrypted BOOLEAN DEFAULT FALSE NOT NULL;
