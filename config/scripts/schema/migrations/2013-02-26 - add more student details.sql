-- TAB-308 add course/study details

alter table studydetails add (
	intendedaward nvarchar2(12),
	begindate date,
	enddate date,
	expectedenddate date,
	fundingsource nvarchar2(6),
	courseyearlength number(2),
	sprstatuscode nvarchar2(6),
	enrolmentstatuscode nvarchar2(6),
	modeofattendancecode nvarchar2(10),
	--level nvarchar2(6),
	ugpg nvarchar2(15)
	
);

-- TAB-308 post-deploy:

alter table studydetails set unused column studentstatus;
