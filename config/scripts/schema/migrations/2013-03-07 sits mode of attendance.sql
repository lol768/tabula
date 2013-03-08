-- TAB-308 Sits Status table

CREATE TABLE MODEOFATTENDANCE (
	
	CODE NVARCHAR2(6) NOT NULL,
	SHORTNAME NVARCHAR2(15),
	FULLNAME NVARCHAR2(50),
  	LASTUPDATEDDATE TIMESTAMP,
  
	CONSTRAINT "MODEOFATTENDANCE_PK" PRIMARY KEY ("CODE")
);
