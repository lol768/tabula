-- let's make a fake ADS
DROP TABLE MODULE_REGISTRATION IF EXISTS;
DROP TABLE MODULE_AVAILABILITY IF EXISTS;
DROP TABLE MODULE_ASSESSMENT_DETAILS IF EXISTS;
DROP TABLE MODULE IF EXISTS;
DROP TABLE STUDENT_CURRENT_STUDY_DETAILS IF EXISTS;

CREATE TABLE IF NOT EXISTS MODULE_REGISTRATION
(
  ACADEMIC_YEAR_CODE VARCHAR(6) NOT NULL 
, ASSESSMENT_GROUP VARCHAR(2) 
, CATS integer
, MAV_OCCURRENCE VARCHAR(6) NOT NULL 
, MODULE_CODE VARCHAR(10) NOT NULL 
, MOD_REG_TYPE_CODE integer 
, REGISTRATION_STATUS integer
, SITS_OR_OMR varCHAR(1) 
, SPR_CODE VARCHAR(12) NOT NULL 
);

CREATE TABLE IF NOT EXISTS MODULE_AVAILABILITY
(
  ACADEMIC_YEAR_CODE VARCHAR(6) NOT NULL
, MODULE_CODE VARCHAR(10) NOT NULL
, MAV_OCCURRENCE VARCHAR(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS MODULE_ASSESSMENT_DETAILS
(
  MODULE_CODE VARCHAR(10) NOT NULL
, SEQ VARCHAR(3) NOT NULL
, ASSESSMENT_CODE VARCHAR(6)
, ASSESSMENT_GROUP VARCHAR(2)
, NAME VARCHAR(255)
)

CREATE TABLE IF NOT EXISTS MODULE
(
  MODULE_CODE VARCHAR(10) NOT NULL
, IN_USE CHAR(1)
, DEPARTMENT_CODE VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS STUDENT_CURRENT_STUDY_DETAILS
(
  UNIVERSITY_ID VARCHAR(7) NOT NULL
, SPR_CODE VARCHAR(12)
, SITS_COURSE_CODE VARCHAR(10)
, LEVEL_CODE VARCHAR(6)
, YEAR_OF_STUDY VARCHAR(6)
, MODE_OF_ATTENDANCE VARCHAR(6)
, ENROLMENT_STATUS VARCHAR(6)
, COURSE_STATUS VARCHAR(6)
, SOURCE_OF_FUNDING VARCHAR(3)
, STUDENT_STATUS VARCHAR(6)
, REGISTRATION_STATUS_CODE INTEGER
, TUTOR1 VARCHAR(12)
, TUTOR2 VARCHAR(12)
, TUTOR3 VARCHAR(12)
, TUTOR4 VARCHAR(12)
, PROGRAMME_OF_STUDY VARCHAR(12)
, ROUTE_CODE VARCHAR(12)
, ACADEMIC_YEAR_CODE VARCHAR(6)
, CATEGORY VARCHAR(1)
, EXTERNAL_QUALIFICATION_OBTAIND VARCHAR(6)
, DEPARTMENT VARCHAR(5)
, COURSE_START_ACADEMIC_YR VARCHAR(6)
, BASE_COURSE_START_AYR VARCHAR(6)
, MODIFIED_DATE DATE
, COURSE_END_DATE DATE
, LAST_COURS_ATTNDCE_DT DATE
, COURSE_REQUIRED_DT DATE
, TRANSFER_CODE_REASON VARCHAR(6)
);

-- unique constraint as found on ADS
CREATE UNIQUE INDEX AS16_CSITE ON MODULE_REGISTRATION(ACADEMIC_YEAR_CODE, MODULE_CODE, MAV_OCCURRENCE, SPR_CODE);

-- Thoughts - only the assignment importer test really needs all this data,
-- so perhaps move it into a separate file. Alternatively, just don't invoke
-- ads.sql at all in the regular PersistenceTestBase since we only require
-- an empty but functional datasource there.

INSERT INTO MODULE VALUES ('CH115-30', 'Y', 'CH'); -- live module, students
INSERT INTO MODULE VALUES ('CH120-15', 'Y', 'CH'); -- live module, students
INSERT INTO MODULE VALUES ('CH130-15', 'Y', 'CH'); -- live module, no students
INSERT INTO MODULE VALUES ('CH130-20', 'Y', 'CH'); -- live module, no students
INSERT INTO MODULE VALUES ('XX101-30', 'N', 'XX'); -- inactive module

-- no students registered on CH130, so should show up in list of empty groups
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH130-15', 'A');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH130-20', 'A');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH130-15', 'A01', 'A', 'A', 'Chem 130 A01');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH130-20', 'A01', 'A', 'A', 'Chem 130 A01 (20 CATS)');

-- some more items that don't have corresponding students,
-- but don't have the right data in other tables to form a complete entry
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('XX100-30', 'A01', 'A', 'A', 'Mystery Meat');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'XX100-30', 'A');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('XX101-30', 'A01', 'A', 'A', 'Danger Zone');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'XX101-30', 'A');

INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH115-30', 'A01', 'A', 'A', 'Chemicals Essay');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH115-30', 'A');

INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH120-15', 'A01', 'A', 'A', 'Chemistry Dissertation');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH120-15', 'A');

insert into STUDENT_CURRENT_STUDY_DETAILS (UNIVERSITY_ID, SPR_CODE, STUDENT_STATUS) values ('0123456', '0123456/1', 'C');
insert into STUDENT_CURRENT_STUDY_DETAILS (UNIVERSITY_ID, SPR_CODE, STUDENT_STATUS) values ('0123457', '0123457/1', 'C');
insert into STUDENT_CURRENT_STUDY_DETAILS (UNIVERSITY_ID, SPR_CODE, STUDENT_STATUS) values ('0123458', '0123458/1', 'C');
insert into STUDENT_CURRENT_STUDY_DETAILS (UNIVERSITY_ID, SPR_CODE, STUDENT_STATUS) values ('0123459', '0123459/1', 'P');

insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123456/1');
insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123457/1');
insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123458/1');
insert into module_registration values ('11/12',NULL,30,'A','CH115-30',1,1,'S','0123460/1');
insert into module_registration values ('11/12','A',30,'A','CH120-15',1,1,'S','0123458/1');

-- Some data from other years that the import should ignore

insert into module_registration values ('10/11','A',30,'A','CH130-20',1,1,'S','0123458/1');
