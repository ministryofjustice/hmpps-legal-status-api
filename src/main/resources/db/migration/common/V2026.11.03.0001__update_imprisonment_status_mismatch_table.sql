--- Amend the IMPRISONMENT_STATUS_MISMATCH table
--- BookingId used to be stored as a BIGINT, but needs to be changed to a text value,
--- as that is what is received from an upstream service.
--- This cast will change all the existing numerical data to it's text form.

ALTER TABLE IMPRISONMENT_STATUS_MISMATCH
    ALTER COLUMN BOOKING_ID TYPE VARCHAR(20)
    USING BOOKING_ID::VARCHAR;
