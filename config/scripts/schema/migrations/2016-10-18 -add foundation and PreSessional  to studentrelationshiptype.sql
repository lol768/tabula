--TAB-4639
alter table STUDENTRELATIONSHIPTYPE add (
  EXPECTED_FOUNDATION NUMBER(1,0) DEFAULT 0 NOT NULL,
	EXPECTED_PRESESSIONAL NUMBER(1,0) DEFAULT 0 NOT NULL
);