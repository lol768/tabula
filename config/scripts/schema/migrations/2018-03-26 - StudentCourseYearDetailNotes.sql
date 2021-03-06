CREATE TABLE STUDENTCOURSEDETAILSNOTE (
  CODE nvarchar2(255) NOT NULL,
  SCJCODE nvarchar2(255),
  NOTE NCLOB,
  CONSTRAINT "SCDNOTE_PK" PRIMARY KEY ("CODE")
);

ALTER TABLE STUDENTCOURSEDETAILSNOTE ADD CONSTRAINT "SCDNOTE_FK" FOREIGN KEY (SCJCODE) REFERENCES STUDENTCOURSEDETAILS(SCJCODE);
CREATE INDEX IDX_SCDNOTE_STUDENTCOURSEDETAILS ON STUDENTCOURSEDETAILSNOTE(SCJCODE);