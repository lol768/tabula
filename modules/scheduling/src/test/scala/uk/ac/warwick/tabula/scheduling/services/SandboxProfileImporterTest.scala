package uk.ac.warwick.tabula.scheduling.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Fixtures
import org.joda.time.LocalDate
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.scheduling.helpers.ImportRowTracker

class SandboxProfileImporterTest extends TestBase {

	val importer = new SandboxProfileImporter

	@Test def itWorks() = withFakeTime(new DateTime(2013, DateTimeConstants.JULY, 4, 11, 27, 54, 0)) {
		val department = Fixtures.department("hom", "History of Music")
		val macs = importer.membershipInfoByDepartment(department)
		macs.size should be (310)

		val mac = macs(0)
		mac.photo() should be ('empty)

		val member = mac.member
		member.universityId should be ("4300001")
		member.departmentCode should be ("hom")
		member.email should be ("R.Davis@tabula-sandbox.warwick.ac.uk")
		member.targetGroup should be ("Undergraduate - full-time")
		member.title should be ("Miss")
		member.preferredForenames should be ("Rachel")
		member.preferredSurname should be ("Davis")
		member.position should be ("Undergraduate - full-time")
		member.dateOfBirth should be (new LocalDate(1994, DateTimeConstants.MARCH, 11))
		member.usercode should be ("hom0001")
		member.startDate should be (new LocalDate(2012, DateTimeConstants.JULY, 4))
		member.endDate should be (new LocalDate(2015, DateTimeConstants.JULY, 4))
		member.modified should be (new DateTime(2013, DateTimeConstants.JULY, 4, 11, 27, 54, 0))
		member.phoneNumber should be (null)
		member.gender should be (Gender.Female)
		member.alternativeEmailAddress should be (null)
		member.userType should be (MemberUserType.Student)

		val cmds = importer.getMemberDetails(Seq(mac), Map(), new ImportRowTracker)
		cmds.size should be (1)

		val cmd = cmds(0)
	}

}