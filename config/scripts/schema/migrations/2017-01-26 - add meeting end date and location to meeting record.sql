-- TAB-2605
--
ALTER TABLE MEETINGRECORD ADD MEETING_END_DATE TIMESTAMP;
ALTER TABLE MEETINGRECORD ADD MEETING_LOCATION NVARCHAR2(255);
UPDATE MEETINGRECORD SET MEETING_END_DATE = (MEETING_DATE + INTERVAL '1' HOUR) WHERE MEETING_END_DATE IS NULL;