package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{StudentProperties, Member, OtherMember, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{PropertyCopying, ImportRowTracker}
import uk.ac.warwick.tabula.scheduling.services._
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.userlookup.User


object ImportStudentRowCommand {
	def apply(
		member: MembershipInformation,
		ssoUser: User,
		resultSet: ResultSet,
		importRowTracker: ImportRowTracker,
		importStudentCourseCommand: ImportStudentCourseCommand
	) = {
			new ImportStudentRowCommandInternal(member, ssoUser, resultSet, importRowTracker, importStudentCourseCommand)
				with Command[Member]
				with AutowiringProfileServiceComponent
				with AutowiringTier4RequirementImporterComponent
				with AutowiringModeOfAttendanceImporterComponent
				with Unaudited
			}
}


/*
 * ImportStudentRowCommand takes a number of other commands as arguments which perform sub-tasks.
 * These need to be passed in, rather than newed up within the command, to enable testing
 * without auto-wiring.
 */
class ImportStudentRowCommandInternal(
	val member: MembershipInformation,
	val ssoUser: User,
	val resultSet: ResultSet,
	val importRowTracker: ImportRowTracker,
	var importStudentCourseCommand: ImportStudentCourseCommand
)

extends ImportMemberCommand(member, ssoUser, Some(resultSet))
with Describable[Member]
with ImportStudentRowCommandState
with PropertyCopying
with Logging {

	self:ProfileServiceComponent with Tier4RequirementImporterComponent with ModeOfAttendanceImporterComponent  =>

	// import ImportMemberHelpers._

	implicit val rs = resultSet

	// populate *before* apply to avoid concurrency problems
	this.nationality = rs.getString("nationality")
	this.mobileNumber = rs.getString("mobile_number")
	val disabilityCode = rs.getString("disability")

	override def applyInternal(): Member = {
		transactional() {
			// set appropriate disability object iff a non-null code is retrieved - I <3 scala options
			profileService.getDisability(disabilityCode).foreach(this.disability = _)

			val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

			logger.debug("Importing member " + universityId + " into " + memberExisting)

			val (isTransient, member) = memberExisting match {
				case Some(member: StudentMember) => (false, member)
				case Some(member: OtherMember) => {
					// TAB-692 delete the existing member, then return a brand new one
					memberDao.delete(member)
					(true, new StudentMember(universityId))
				}
				case Some(member) => throw new IllegalStateException("Tried to convert " + member + " into a student!")
				case _ => (true, new StudentMember(universityId))
			}

			if (!importRowTracker.universityIdsSeen.contains(member.universityId)) {
				saveStudentDetails(isTransient, member)
			}

			importStudentCourseCommand.stuMem = member
			val studentCourseDetails = importStudentCourseCommand.apply()

			member.attachStudentCourseDetails(studentCourseDetails)

			importRowTracker.universityIdsSeen.add(member.universityId)

			member
		}
	}

	private def saveStudentDetails(isTransient: Boolean, member: StudentMember) {
		// There are multiple rows returned by the SQL per student; only import non-course details if we haven't already
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		val tier4VisaRequirement = tier4RequirementImporter.hasTier4Requirement(universityId)

		// We intentionally use single pipes rather than double here - we want all statements to be evaluated
		val hasChanged = (copyMemberProperties(commandBean, memberBean)
			| copyStudentProperties(commandBean, memberBean)
			| markAsSeenInSits(memberBean)
			|| (member.tier4VisaRequirement != tier4VisaRequirement))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			member.tier4VisaRequirement = tier4VisaRequirement
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber", "disability"
	)

	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}

trait ImportStudentRowCommandState extends StudentProperties