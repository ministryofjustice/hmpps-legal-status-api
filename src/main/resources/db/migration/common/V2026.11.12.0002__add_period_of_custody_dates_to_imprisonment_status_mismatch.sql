-- Amend the IMPRISONMENT_STATUS_MISMATCH table
-- PREVIOUS_PERIOD_CONCLUDED_AT is the concludedAt of the prisoner's previous period of custody, from the Person Location API.
-- CHARGES_WINDOW_START is the fromDate used to get the prisoner's charges from the Remand and Sentencing API.
-- It is selected from PREVIOUS_PERIOD_CONCLUDED_AT: the date on which the previous period concluded, plus 1 day.
-- Both are null when there is no previous period of custody (then no fromDate was used).

ALTER TABLE IMPRISONMENT_STATUS_MISMATCH
    ADD COLUMN PREVIOUS_PERIOD_CONCLUDED_AT TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN CHARGES_WINDOW_START DATE NULL;
