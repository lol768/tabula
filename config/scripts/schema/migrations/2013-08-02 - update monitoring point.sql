drop table monitoringpointset;

alter table monitoringpointsetyear
	rename to monitoringpointset;

alter table monitoringpointset
	modify year number(4,0);

alter table monitoringpointset
	add (
		createddate timestamp(6),
		updateddate timestamp(6),
		templatename nvarchar2(255) not null
	);

alter table monitoringpoint
	drop column position;
