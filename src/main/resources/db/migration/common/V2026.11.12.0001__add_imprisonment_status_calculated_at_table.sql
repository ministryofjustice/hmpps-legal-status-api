--- Create the IMPRISONMENT_STATUS_CALCULATED_AT table
--- This is a temporary table whilst we flesh out other parts of the code, and will be used to store a timestamp for
--- when a prisoners Imprisonment Status was calculated.
--- This is to prevent multiple trigger events in quick succession retriggering the calculation unnecessarily.
--- When we implement the final table for storing Imprisonment Statuses it will also CALCULATED_AT for the same reason
--- but implementing this temporary table now means we can proceed with the project and not think about the design
--- of the final table until we need it/

CREATE TABLE IMPRISONMENT_STATUS_CALCULATED_AT
(
    PRISON_NUMBER VARCHAR(10) NOT NULL PRIMARY KEY,
    CALCULATED_AT TIMESTAMP   NOT NULL
);
COMMENT
ON TABLE IMPRISONMENT_STATUS_CALCULATED_AT IS 'Temporary table containing a timestamp for when a prisoners Imprisonment Status was calculated';
