--- Create the IMPRISONMENT_STATUS_MISMATCH table
--- IMPRISONMENT_STATUS_MISMATCH records each Imprisonment Status calculation whose result did not match the NOMIS
--- Imprisonment Status, with a point-in-time snapshot of the NOMIS and RaS data, so that the mismatch can be reproduced
--- and fixed after the prisoner's data has changed.

CREATE TABLE IMPRISONMENT_STATUS_MISMATCH
(
    ID                        UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    PRISON_NUMBER             VARCHAR(10)              NOT NULL,
    BOOKING_ID                BIGINT,
    RECEPTION_DATE            DATE,
    NOMIS_IMPRISONMENT_STATUS VARCHAR(50)              NOT NULL,
    NOMIS_CALCULATED_AT       TIMESTAMP WITH TIME ZONE,
    DPS_IMPRISONMENT_STATUS   VARCHAR(50)              NOT NULL REFERENCES IMPRISONMENT_STATUS (IMPRISONMENT_STATUS),
    DPS_MAIN_OFFENCE_CODE     VARCHAR(20),
    DPS_CALCULATED_FROM       VARCHAR(20)              NOT NULL,
    DPS_CALCULATED_AT         TIMESTAMP WITH TIME ZONE NOT NULL,
    RAS_DATA                  JSONB                    NOT NULL,
    OFFENCE_SEVERITIES        JSONB                    NOT NULL,
    NOMIS_DATA                JSONB                    NOT NULL,
    CREATED_AT                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IMPRISONMENT_STATUS_MISMATCH_PRISON_NUMBER_IDX ON IMPRISONMENT_STATUS_MISMATCH (PRISON_NUMBER);

COMMENT ON TABLE IMPRISONMENT_STATUS_MISMATCH IS 'Imprisonment Status calculations that did not match NOMIS, with a snapshot of the NOMIS and RaS data used. The ID is the reference sent to Slack/Jira.';
COMMENT ON COLUMN IMPRISONMENT_STATUS_MISMATCH.NOMIS_IMPRISONMENT_STATUS IS 'NOMIS code. Deliberately not a foreign key, as NOMIS may return statuses that are not in IMPRISONMENT_STATUS.';
COMMENT ON COLUMN IMPRISONMENT_STATUS_MISMATCH.RAS_DATA IS 'Raw Remand and Sentencing API responses used in the calculation.';
COMMENT ON COLUMN IMPRISONMENT_STATUS_MISMATCH.OFFENCE_SEVERITIES IS 'Manage Offences severity ranking per offence code used in the calculation.';
COMMENT ON COLUMN IMPRISONMENT_STATUS_MISMATCH.NOMIS_DATA IS 'NOMIS sentence and offence data at the time of the mismatch, to compare with RAS_DATA.';
