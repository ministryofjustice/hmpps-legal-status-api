-- Amend the IMPRISONMENT_STATUS_MISMATCH table (IS-181)
-- NOT_CALCULATED_REASON flags a row where we did not calculate the Imprisonment Status, and tells why.
-- NULL means a normal mismatch: we calculated a status and it is different from the NOMIS status.
-- A flagged row has no DPS Imprisonment Status and no DPS_CALCULATED_FROM, so these columns are now nullable.
-- The check constraint makes sure that a row is either a normal mismatch or a flagged row, never part of each.
-- Existing rows get NULL, so they stay normal mismatches.
--
-- ASSUMPTION (IS-181): an empty list of charges from the Remand and Sentencing API is never valid. A prisoner in
-- custody always has at least one charge. So an empty response means that the RaS data is not complete (for example,
-- RaS has not processed all the events yet). We record it with the reason NO_RAS_CHARGES and do not calculate.
-- See LegacyDataService for what could make this assumption wrong.
--
-- To query only the real mismatches, use: WHERE NOT_CALCULATED_REASON IS NULL

ALTER TABLE IMPRISONMENT_STATUS_MISMATCH
    ADD COLUMN NOT_CALCULATED_REASON VARCHAR(50) NULL,
    ALTER COLUMN DPS_IMPRISONMENT_STATUS DROP NOT NULL,
    ALTER COLUMN DPS_CALCULATED_FROM DROP NOT NULL,
    ADD CONSTRAINT IMPRISONMENT_STATUS_MISMATCH_CALCULATED_CHK CHECK (
        (NOT_CALCULATED_REASON IS NULL AND DPS_IMPRISONMENT_STATUS IS NOT NULL AND DPS_CALCULATED_FROM IS NOT NULL)
        OR (NOT_CALCULATED_REASON IS NOT NULL AND DPS_IMPRISONMENT_STATUS IS NULL AND DPS_CALCULATED_FROM IS NULL)
    );

COMMENT ON COLUMN IMPRISONMENT_STATUS_MISMATCH.NOT_CALCULATED_REASON IS 'NULL for a normal mismatch. Otherwise, why the Imprisonment Status was not calculated (e.g. NO_RAS_CHARGES: RaS returned no charges, which we assume is never valid). Flagged rows are not mismatches.';
