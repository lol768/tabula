package uk.ac.warwick.tabula.data.model
import scala.reflect.BeanProperty

import scala.reflect.BeanProperty
import org.hibernate.annotations.{AccessType, Type}
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.joda.time.LocalDate
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.permissions.PermissionsTarget


@Entity
@AccessType("field")
class StudyDetails extends StudyDetailsProperties with ToString with HibernateVersioned with PermissionsTarget {
	
	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	var student: StudentMember = _
	
	@GenericGenerator(name = "generator", strategy = "foreign", parameters = Array(new Parameter(name="property", value = "student")))
	@GeneratedValue(generator = "generator")
	@Id @BeanProperty var universityId: String = _
	
	def id = universityId
	
	def toStringProps = Seq("student" -> student)
	
	def permissionsParents = Seq(Option(student)).flatten
}

trait StudyDetailsProperties {
	
	@BeanProperty var sprCode: String = _
	@BeanProperty var sitsCourseCode: String = _
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	@BeanProperty var route: Route = _
	
	@ManyToOne
	@JoinColumn(name = "study_department_id")
	@BeanProperty var studyDepartment: Department = _
	
	@BeanProperty var yearOfStudy: JInteger = _

	@BeanProperty var fundingSource: String = _
	
	@BeanProperty var intendedAward: String = _

	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var beginDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var endDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var expectedEndDate: LocalDate = _
	
	@BeanProperty var courseYearLength: String = _	

	//@BeanProperty var sprStatusCode: String = _
	@ManyToOne
	@JoinColumn(name="sprStatusCode", referencedColumnName="code")
	@BeanProperty var sprStatus: SitsStatus = _
	
	//@BeanProperty var enrolmentStatusCode: String = _
	@ManyToOne
	@JoinColumn(name="enrolmentStatusCode", referencedColumnName="code")
	@BeanProperty var enrolmentStatus: SitsStatus = _
	

	//@BeanProperty var levelCode: String = _
	
	@ManyToOne
	@JoinColumn(name="modeOfAttendanceCode", referencedColumnName="code")	
	@BeanProperty var modeOfAttendance: ModeOfAttendance = _
	
	@BeanProperty var ugPg: String = _
	
//	@BeanProperty var attendanceMode: String = _
//	
//	@BeanProperty var programmeOfStudy: String = _	
//	
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	@BeanProperty var academicYear: AcademicYear = _
//	
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	@BeanProperty var courseStartYear: AcademicYear = _
//	
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	@BeanProperty var yearCommencedDegree: AcademicYear = _
//	
//	@Basic
//	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
//	@BeanProperty var courseBaseYear: AcademicYear = _
//	
//	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
//	@BeanProperty var courseEndDate: LocalDate = _
	
//	@BeanProperty var transferReason: String = _

//	
//	@BeanProperty var feeStatus: String = _
//	@BeanProperty var domicile: String = _
//	@BeanProperty var highestQualificationOnEntry: String = _
//	
//	@BeanProperty var lastInstitute: String = _
//	@BeanProperty var lastSchool: String = _	
	
}