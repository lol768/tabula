package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.Date
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.junit.Ignore
import org.junit.Test
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDaoImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.Gender._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.RelationshipType
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.MembershipMember
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporterImpl
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.ProfileServiceImpl
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.StudentCourseDetailsDaoImpl
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.Fixtures
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.DepartmentDaoImpl
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.data.SitsStatusDao

// scalastyle:off magic.number
@DirtiesContext(classMode=AFTER_EACH_TEST_METHOD)
class ImportSingleStudentRowCommandTest extends AppContextTestBase with Mockito with Logging {

	@Autowired var memberDao:MemberDao =_
	@Autowired var moaDao:ModeOfAttendanceDao =_
	@Autowired var sitsStatusDao:SitsStatusDao =_

	trait Environment {
		val blobBytes = Array[Byte](1,2,3,4,5)

		val fileDao = smartMock[FileDao]

		val route = new Route
		val modAndDeptService = smartMock[ModuleAndDepartmentService]
		val courseAndRouteService = smartMock[CourseAndRouteService]
		courseAndRouteService.getRouteByCode("c100") returns (Some(route))

		val department = new Department
		department.code = "ph"
		department.name = "Philosophy"
		department.personalTutorSource = Department.Settings.PersonalTutorSourceValues.Sits
		modAndDeptService.getDepartmentByCode("ph") returns (Some(department))
		modAndDeptService.getDepartmentByCode("PH") returns (Some(department))
		val rs = smartMock[ResultSet]
		val md = smartMock[ResultSetMetaData]
		rs.getMetaData() returns(md)
		md.getColumnCount() returns(4)
		md.getColumnName(1) returns("gender")
		md.getColumnName(2) returns("year_of_study")
		md.getColumnName(3) returns("spr_code")
		md.getColumnName(4) returns("route_code")

		rs.getString("gender") returns("M")
		rs.getInt("year_of_study") returns(3)
		rs.getString("spr_code") returns("0672089/2")
		rs.getString("route_code") returns("C100")
		rs.getString("spr_tutor1") returns ("IN0070790")
		rs.getString("homeDepartmentCode") returns ("PH")
		rs.getString("scj_code") returns ("0672089/2")
		rs.getDate("begin_date") returns new Date(new java.util.Date("12 May 2011").getTime())
		rs.getDate("end_date") returns new Date(new java.util.Date("12 May 2014").getTime())
		rs.getDate("expected_end_date") returns new Date(new java.util.Date("12 May 2015").getTime())
		rs.getInt("sce_sequence_number") returns (1)
		rs.getString("enrolment_status_code") returns ("F")
		rs.getString("mode_of_attendance_code") returns ("P")
		rs.getString("sce_academic_year") returns ("10/11")

		val mm = MembershipMember(
			universityId 			= "0672089",
			departmentCode			= "ph",
			email					= "M.Mannion@warwick.ac.uk",
			targetGroup				= null,
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			position				= null,
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			startDate				= null,
			endDate					= null,
			modified				= null,
			phoneNumber				= null,
			gender					= null,
			alternativeEmailAddress	= null,
			userType				= Student		)

		val mac = MembershipInformation(mm, () => Some(blobBytes))

		val modeOfAttendanceImporter = smartMock[ModeOfAttendanceImporter]
		val profileService = smartMock[ProfileService]
		//val studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]

		var studentCourseDetailsDao = new StudentCourseDetailsDaoImpl
		var studentCourseYearDetailsDao = new StudentCourseYearDetailsDaoImpl

		val sitsStatusesImporter = smartMock[SitsStatusesImporter]

		modeOfAttendanceImporter.modeOfAttendanceMap returns Map("F" -> new ModeOfAttendance("F", "FT", "Full Time"), "P" -> new ModeOfAttendance("P", "PT", "Part Time"))
		modeOfAttendanceImporter.getModeOfAttendanceForCode("P") returns Some(new ModeOfAttendance("P", "PT", "Part Time"))
		sitsStatusesImporter.sitsStatusMap returns Map("F" -> new SitsStatus("F", "F", "Fully Enrolled"), "P" -> new SitsStatus("P", "P", "Permanently Withdrawn"))
	}

	@Test def testImportSingleStudentCourseYearCommand {
		new Environment {
			val yearCommand = new ImportSingleStudentCourseYearCommand(rs)
			yearCommand.modeOfAttendanceImporter = modeOfAttendanceImporter
			yearCommand.profileService = profileService
			yearCommand.sitsStatusesImporter = sitsStatusesImporter

			studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDaoImpl]
			studentCourseDetailsDao = smartMock[StudentCourseDetailsDaoImpl]

			val studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"

			yearCommand.studentCourseYearDetailsDao = studentCourseYearDetailsDao
			yearCommand.studentCourseDetails = studentCourseDetails

			// now the set up is done, run the apply command and test it:
			val studentCourseYearDetails = yearCommand.applyInternal()
			studentCourseYearDetails.academicYear.toString should be ("10/11")
			studentCourseYearDetails.sceSequenceNumber should be (1)
			studentCourseYearDetails.enrolmentStatus.code should be ("F")
			studentCourseYearDetails.lastUpdatedDate should not be null
			studentCourseYearDetails.modeOfAttendance.code should be ("P")
			studentCourseYearDetails.yearOfStudy should be (3)

			there was one(studentCourseYearDetailsDao).saveOrUpdate(any[StudentCourseYearDetails]);
		}
	}

	@Test def testImportSingleStudentCourseCommand {
		new Environment {
			val yearCommand = new ImportSingleStudentCourseYearCommand(rs)
			yearCommand.modeOfAttendanceImporter = modeOfAttendanceImporter
			yearCommand.profileService = profileService
			yearCommand.sitsStatusesImporter = sitsStatusesImporter

			studentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDaoImpl]
			studentCourseDetailsDao = smartMock[StudentCourseDetailsDaoImpl]

			// first set up the studentCourseYearDetails as above
			var studentCourseDetails = new StudentCourseDetails
			studentCourseDetails.scjCode = "0672089/2"
			studentCourseDetails.sprCode = "0672089/2"
			yearCommand.studentCourseYearDetailsDao = studentCourseYearDetailsDao
			yearCommand.studentCourseDetails = studentCourseDetails
			val studentCourseYearDetails = yearCommand.applyInternal()

			// then set up and run importSingleStudentCourseDetailsCommand apply
			val command = new ImportSingleStudentCourseCommand(rs, yearCommand)
			command.studentCourseDetailsDao = studentCourseDetailsDao
			command.sitsStatusesImporter = sitsStatusesImporter

			// now the set up is done, run the apply command and test it:
			studentCourseDetails = command.applyInternal()

			// now test some stuff
			studentCourseDetails.scjCode should be ("0672089/2")
			studentCourseDetails.beginDate.toString should be ("2011-05-12")
			studentCourseDetails.endDate.toString should be ("2014-05-12")
			studentCourseDetails.expectedEndDate.toString should be ("2015-05-12")

			//studentCourseDetails.studentCourseYearDetails.size should be (1)

			there was one(studentCourseDetailsDao).saveOrUpdate(any[StudentCourseDetails]);
		}
	}

	@Test
	def testImportSingleStudentRowCommandWorksWithNew {
		new Environment {
			transactional { tx =>
				// first set up the commands, starting at the year-specific end:
				val yearCommand = new ImportSingleStudentCourseYearCommand(rs)
				yearCommand.modeOfAttendanceImporter = modeOfAttendanceImporter
				yearCommand.profileService = profileService
				yearCommand.sitsStatusesImporter = sitsStatusesImporter

				// now the course-specific command:
				val courseCommand = new ImportSingleStudentCourseCommand(rs, yearCommand)
				courseCommand.studentCourseDetailsDao = studentCourseDetailsDao
				courseCommand.sitsStatusesImporter = sitsStatusesImporter

				val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs, courseCommand)

				command.memberDao = memberDao
				command.fileDao = fileDao
				command.moduleAndDepartmentService = modAndDeptService

				// need to save the transient department as we're saving a reference to it in member
				val deptDao = new DepartmentDaoImpl
				deptDao.save(department)

				moaDao.saveOrUpdate(new ModeOfAttendance("F", "FT", "Full Time"))
				moaDao.saveOrUpdate(new ModeOfAttendance("P", "PT", "Part Time"))

				sitsStatusDao.saveOrUpdate(new SitsStatus("F", "Fully enrolled", "Fully enrolled for this session"))
				sitsStatusDao.saveOrUpdate(new SitsStatus("P", "PWD", "Permanently Withdrawn"))

				// now the set-up is done, run the apply command for member, which should cascade and run the other apply commands:
				val member = command.applyInternal()
				session.flush()

				// test that member contains the expected data:
				member.title should be ("Mr")
				member.universityId should be ("0672089")
				member.userId should be ("cuscav")
				member.email should be ("M.Mannion@warwick.ac.uk")
				member.gender should be (Male)
				member.firstName should be ("Mathew")
				member.lastName should be ("Mannion")
				member.photo should not be (null)
				member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

/*				val uniId = member.universityId

				val memFromDb = memberDao.getByUserId(uniId)

				val studentMember = (memFromDb match {
					case Some(stu: StudentMember) => Some(stu)
					case _ => None
				}).get

				studentMember.studentCourseDetails.size should be (1)

				there was one(fileDao).savePermanent(any[FileAttachment])
				there was no(fileDao).saveTemporary(any[FileAttachment])

				there was one(memberDao).saveOrUpdate(any[Member])*/
			}
		}
	}

	/*
		@Test def worksWithExisting {
		new Environment {
			// now set up studentCourseDetail:
			val studentCourseDetailsDao = smartMock[StudentCourseDetailsDao]

			val courseCommand = new ImportSingleStudentCourseCommand(rs, yearCommand)
			courseCommand.studentCourseDetailsDao = studentCourseDetailsDao
			courseCommand.sitsStatusesImporter = sitsStatusesImporter

			// end of setting up studentCourseDetail

			val existing = new StudentMember("0672089")

			val memberDao = smartMock[MemberDao]
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs, courseCommand)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds

			// now the set up is done, run the apply command and test it:
			val member = command.applyInternal()
			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.photo should not be (null)
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))

			there was one(fileDao).savePermanent(any[FileAttachment])
			there was no(fileDao).saveTemporary(any[FileAttachment])

			there was one(memberDao).saveOrUpdate(existing)
		}
	}

	@Transactional
	@Test def testCaptureTutorIfSourceIsLocal {

		new Environment {
			// now set up studentCourseDetail:
			val studentCourseDetailsDao = smartMock[StudentCourseDetailsDao]

			val courseCommand = new ImportSingleStudentCourseCommand(rs, yearCommand)
			courseCommand.studentCourseDetailsDao = studentCourseDetailsDao
			courseCommand.sitsStatusesImporter = sitsStatusesImporter

			// end of setting up studentCourseDetail

			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")
			val memberDao = smartMock[MemberDao]
			val relationshipService = smartMock[RelationshipService]
			//val profileService = smartMock[ProfileService]

			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))

			// if personalTutorSource is "local", there should be no update
			department.personalTutorSource = "local"

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs, courseCommand)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds
			command.profileService = profileService

			val member = command.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.studentCourseDetails.size should not be (0)

			there was no(relationshipService).saveStudentRelationship(PersonalTutor, "0672089/2","0070790");
		}
	}

	@Ignore("broken") @Transactional
	@Test def testCaptureTutorIfSourceIsSits {

		new Environment {
			val existing = new StudentMember("0672089")
			val existingStaffMember = new StaffMember("0070790")
			val memberDao = smartMock[MemberDao]
			val profileService = smartMock[ProfileService]
			val relationshipService = smartMock[RelationshipService]


			memberDao.getByUniversityId("0070790") returns(Some(existingStaffMember))
			memberDao.getByUniversityId("0672089") returns(Some(existing))
			relationshipService.findCurrentRelationships(RelationshipType.PersonalTutor, "0672089/2") returns (Nil)

			// if personalTutorSource is "SITS", there *should* an update
			department.personalTutorSource = Department.Settings.PersonalTutorSourceValues.Sits

			val command = new ImportSingleStudentRowCommand(mac, new AnonymousUser(), rs)
			command.memberDao = memberDao
			command.fileDao = fileDao
			command.moduleAndDepartmentService = mds
			command.profileService = profileService


			val member = command.applyInternal() match {
				case stu: StudentMember => Some(stu)
				case _ => None
			}

			val studentMember = member.get

			studentMember.mostSignificantCourseDetails should not be (null)

			there was one(relationshipService).saveStudentRelationship(PersonalTutor, "0672089/2","0070790");
		}
	}*/
}

