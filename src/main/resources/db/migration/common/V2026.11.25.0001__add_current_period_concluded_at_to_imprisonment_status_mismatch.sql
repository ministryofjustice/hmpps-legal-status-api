-- Amend the IMPRISONMENT_STATUS_MISMATCH table
-- CURRENT_PERIOD_CONCLUDED_AT is the concludedAt of the prisoner's current period of custody, from the Person Location API. IE. their release date

ALTER TABLE IMPRISONMENT_STATUS_MISMATCH
    ADD COLUMN CURRENT_PERIOD_CONCLUDED_AT TIMESTAMP WITHOUT TIME ZONE NULL;
