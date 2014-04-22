package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Award, AccreditedPriorLearning, StudentCourseDetails, Module}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.spring.Wire

trait AccreditedPriorLearningDaoComponent {
	val accreditedPriorLearningDao: AccreditedPriorLearningDao
}

trait AutowiringAccreditedPriorLearningDaoComponent extends AccreditedPriorLearningDaoComponent {
	val accreditedPriorLearningDao = Wire[AccreditedPriorLearningDao]
}

trait AccreditedPriorLearningDao {
	def saveOrUpdate(accreditedPriorLearning: AccreditedPriorLearning)
	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, award: Award, sequenceNumber: Integer): Option[AccreditedPriorLearning]
	def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear) : Seq[AccreditedPriorLearning]
}

@Repository
class AccreditedPriorLearningDaoImpl extends AccreditedPriorLearningDao with Daoisms {

	def saveOrUpdate(accreditedPriorLearning: AccreditedPriorLearning) = session.saveOrUpdate(accreditedPriorLearning)

	def getByNotionalKey(
												studentCourseDetails: StudentCourseDetails,
												award: Award,
												sequenceNumber: Integer
												) =
		session.newCriteria[AccreditedPriorLearning]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("award", award))
			.add(is("sequenceNumber", sequenceNumber))
			.uniqueResult

	def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear) : Seq[AccreditedPriorLearning] = {
		val query = session.newQuery[AccreditedPriorLearning]("""
				select distinct mr
					from AccreditedPriorLearning mr
					where academicYear = :academicYear
					and studentCourseDetails.missingFromImportSince is null
					and studentCourseDetails.student.userId in :usercodes
																										 """)
			.setString("academicYear", academicYear.getStoreValue.toString)

		query.setParameterList("usercodes", userCodes)
			.seq
	}
}
