package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AlumniProperties
import uk.ac.warwick.tabula.data.model.MemberProperties
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.model.OtherMember
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.model.StudentCourseYearProperties
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.StudyDetailsProperties
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Logging

import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.helpers.SitsPropertyCopying
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService


class ImportSingleStudentCourseYearCommand(studentCourseDetails: StudentCourseDetails, resultSet: ResultSet)
	extends Command[StudentMember] with Logging with Daoisms
	with StudentCourseYearProperties with Unaudited with PropertyCopying with SitsPropertyCopying {
	import ImportMemberHelpers._

	implicit val rs = resultSet
	implicit val metadata = rs.getMetaData

	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]
	var profileService = Wire.auto[ProfileService]
	var studentCourseYearDetailsDao = Wire.auto[StudentCourseYearDetailsDao]


	// A few intermediate properties that will be transformed later
	var enrolmentStatusCode: String = _
	var modeOfAttendanceCode: String = _

	this.yearOfStudy = rs.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = rs.getInt("sce_sequence_number")

	this.enrolmentStatusCode = rs.getString("enrolment_status_code")
	this.modeOfAttendanceCode = rs.getString("mode_of_attendance_code")


	override def applyInternal(): StudentCourseYearDetails = transactional() {
		val studentCourseYearDetailsExisting = studentCourseYearDetailsDao.getByScjCodeAndSequence(
				studentCourseDetails.scjCode,
				sceSequenceNumber)

		logger.debug("Importing student course details for " + studentCourseDetails.scjCode + ", " + sceSequenceNumber)

		val (isTransient, studentCourseYearDetails) = studentCourseYearDetailsExisting match {
			case Some(studentCourseYearDetails: StudentCourseYearDetails) => (false, studentCourseYearDetails)
			case _ => (true, new StudentCourseYearDetails(studentCourseDetails, sceSequenceNumber))
		}

		val commandBean = new BeanWrapperImpl(this)
		val studentCourseYearDetailsBean = new BeanWrapperImpl(studentCourseYearDetails)

		val hasChanged = copyStudentCourseYearProperties(commandBean, studentCourseYearDetailsBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + studentCourseYearDetails)

			studentCourseYearDetails.lastUpdatedDate = DateTime.now
			studentCourseYearDetailsDao.saveOrUpdate(studentCourseYearDetails)
		}

		studentCourseYearDetails
	}

	private val basicStudyDetailsYearProperties = Set(
		"yearOfStudy",
		//"fundingSource",
		"modeOfAttendance"
	)

	private def copyStudentCourseYearProperties(commandBean: BeanWrapper, studyDetailsYearBean: BeanWrapper) = {
		copyBasicProperties(basicStudyDetailsYearProperties, commandBean, studyDetailsYearBean) |
		copyStatus("enrolmentStatus", enrolmentStatusCode, studyDetailsYearBean) |
		copyModeOfAttendance("modeOfAttendance", modeOfAttendanceCode, studyDetailsYearBean)
	}

	private def copyModeOfAttendance(property: String, code: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: ModeOfAttendance => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no MOA to having an MOA
			memberBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		}
	}

	private def toModeOfAttendance(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			modeOfAttendanceImporter.modeOfAttendanceMap.get(code).getOrElse(null)
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> studentCourseDetails.scjCode).property("sceSequenceNumber" -> sceSequenceNumber)
}
