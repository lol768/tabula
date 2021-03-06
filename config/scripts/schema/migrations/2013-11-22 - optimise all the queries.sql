--- TAB-1579

CREATE INDEX "IDX_USERGROUP_BASEWGRP" ON USERGROUP("BASEWEBGROUP");

---DROP INDEX MARKERFEEDBACK_FILEATTACHMENT;
CREATE INDEX IDX_MARKERFEEDBACK_FEEDBACK ON MARKERFEEDBACKATTACHMENT("MARKER_FEEDBACK_ID");
CREATE INDEX IDX_MARKERFEEDBACK_ATTACHMENT ON MARKERFEEDBACKATTACHMENT("FILE_ATTACHMENT_ID");

CREATE INDEX IDX_SCD_SPRCODE ON STUDENTCOURSEDETAILS("SPRCODE");

ALTER TABLE ASSESSMENTGROUP ADD CONSTRAINT PK_ASSESSMENTGROUP PRIMARY KEY ("ID");
CREATE INDEX IDX_ASSGROUP_ASSIGNMENT ON ASSESSMENTGROUP("ASSIGNMENT_ID");
CREATE INDEX IDX_ASSGROUP_SGS ON ASSESSMENTGROUP("GROUP_SET_ID");

CREATE INDEX IDX_SUBMISSIONVALUE_FEEDBACK ON SUBMISSIONVALUE("FEEDBACK_ID");
CREATE INDEX IDX_SUBMISSIONVALUE_MFEEDBACK ON SUBMISSIONVALUE("MARKER_FEEDBACK_ID");

CREATE INDEX IDX_EXTENSION_ASSIGNMENT ON EXTENSION("ASSIGNMENT_ID");

CREATE INDEX IDX_REL_RELTYPE ON STUDENTRELATIONSHIP("RELATIONSHIP_TYPE");

CREATE INDEX IDX_GRANTEDROLE_GROUP ON GRANTEDROLE("USERGROUP_ID");
CREATE INDEX IDX_GRANTEDPERM_GROUP ON GRANTEDPERMISSION("USERGROUP_ID");