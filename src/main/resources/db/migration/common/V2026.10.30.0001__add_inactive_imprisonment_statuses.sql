--- The Imprisonment Status Mappings references inactive Imprisonment Statuses (legacy NOMIS data)
--- The original import of Imprisonment Statuses did not include inactive statuses
--- This flyway adds a new column to track active, sets all existing records to true, then inserts new
--- records for the inactive statuses.

ALTER TABLE IMPRISONMENT_STATUS
      ADD COLUMN ACTIVE BOOLEAN;
UPDATE IMPRISONMENT_STATUS SET ACTIVE = true;
ALTER TABLE IMPRISONMENT_STATUS
      ALTER COLUMN ACTIVE SET NOT NULL;

INSERT INTO IMPRISONMENT_STATUS (IMPRISONMENT_STATUS, DESCRIPTION, BAND_CODE, RANK_VALUE, IMPRISONMENT_STATUS_SEQ, ACTIVE)
    VALUES  ('A_FINE1','Adult Imprisonment Fine Payment (Time)','3','211', null, false),
            ('A_FINE2','Adult Imprisonment Fine Payment No Time','3','211', null, false),
            ('CFLIFE','Custody for life S8 CJA82 (18-21 Yrs)','1','5', null, false),
            ('CIVIL_CON','Adult Civil Contempt','9','495', null, false),
            ('CRIM_CON','Criminal Contempt','2','168', null, false),
            ('CUSTPLUS','Custody Plus Sentence','2','110', null, false),
            ('INT_CUST','Intermittent Custody','2','110', null, false),
            ('LIFE','Serving Life Imprisonment','1','20', null, false),
            ('POLICE','In Police Cells (not police remand)','15','620', null, false),
            ('RECEP_REMAND','On Remand (reception)','14','20','1', false),
            ('REFER','DTO_YOI Referal','2','191', null, false),
            ('SEC17_3','Conv - Breach Of CSO - CJA 1972','5','370', null, false),
            ('SEC43','Committed to Crown Court Sec43MHA 1983','5','301', null, false),
            ('SEC90','Life - Murder Under 18','1','5', null, false),
            ('SEC90_03','Life - Murder Under 18 CJA03','1','5', null, false),
            ('SENT_EXL','Sentenced - Extended Licence','2','105', null, false),
            ('UNK_CONV','Unknown Convicted and Unsentenced','4','285', null, false),
            ('UNK_CUST','Unknown Custodial Undisposed','14','605', null, false),
            ('UNK_SENT','Unknown Sentenced','2','205', null, false),
            ('YOC_CONT','Y O Civil Contempt','9','498', null, false)
;
