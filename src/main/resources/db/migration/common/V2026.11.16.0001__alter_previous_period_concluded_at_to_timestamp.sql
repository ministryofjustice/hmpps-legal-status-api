-- Amend the IMPRISONMENT_STATUS_MISMATCH table
-- The Person Location API returns the period of custody dates with no offset (for example "2026-03-30T13:48:03").
-- PREVIOUS_PERIOD_CONCLUDED_AT now stores the value as the API returns it: a local date and time with no time zone.
-- Existing values (if any) are converted to UK local time, which we assume the API uses.

ALTER TABLE IMPRISONMENT_STATUS_MISMATCH
    ALTER COLUMN PREVIOUS_PERIOD_CONCLUDED_AT TYPE TIMESTAMP WITHOUT TIME ZONE
    USING PREVIOUS_PERIOD_CONCLUDED_AT AT TIME ZONE 'Europe/London';
