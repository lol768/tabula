package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty
import javax.persistence.CascadeType._
import org.hibernate.annotations.Type
import org.joda.time.LocalDate
import javax.persistence._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.actions.Viewable
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.Filter
import org.hibernate.annotations.AccessType
import uk.ac.warwick.tabula.actions.Searchable
import uk.ac.warwick.tabula.CurrentUser

object Member {
	final val StudentsOnlyFilter = "studentsOnly"
}

/**
 * Represents an assignment within a module, occurring at a certain time.
 *
 * Notes about the notDeleted filter:
 * filters don't run on session.get() but getById will check for you.
 * queries will only include it if it's the entity after "from" and not
 * some other secondary entity joined on. It's usually possible to flip the
 * query around to make this work.
 */
@FilterDef(name = Member.StudentsOnlyFilter, defaultCondition = "usertype = 'S'")
@Filter(name = Member.StudentsOnlyFilter)
@Entity
@AccessType("field")
class Member extends Viewable with Searchable with MemberProperties with StudentProperties with StaffProperties with AlumniProperties with ToString {
	
	def this(user: CurrentUser) = {
		this()
		
		this.userId = user.apparentId
		this.firstName = user.firstName
		this.lastName = user.lastName
		this.universityId = user.universityId
		this.email = user.email
		this.userType = 
			if (user.isStudent) Student
			else if (user.isStaff) Staff
			else Other
	}
	
	def this(id: String) = {
		this()
		this.universityId = id
	}
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var lastUpdatedDate = DateTime.now
	
	@BeanProperty def fullName = firstName + " " + lastName
	@BeanProperty def officialName = title + " " + fullFirstName + " " + lastName
	@BeanProperty def description = {
		def userType = Option(groupName) orElse(Option(""))
		def courseName = Option(route) map (", " + _.name) orElse(Option(""))
		def deptName = Option(homeDepartment) map (", " + _.name) orElse(Option(""))
		 
		userType.get + courseName.get + deptName.get
	}
	
	/** 
	 * Get all departments that this student is affiliated with. This includes their home department, 
	 * the department running their course and any departments that they are taking modules in.
	 */
	def affiliatedDepartments = {
		val depts = Set(Option(homeDepartment), Option(studyDepartment), Option(route).map(x=> x.department))
		
		depts.flatten.toSeq
	}

	def asSsoUser = {
		val u = new User
		u.setUserId(userId)
		u.setWarwickId(universityId)
		u.setFirstName(firstName)
		u.setLastName(lastName)
		u.setFullName(fullName)
		u.setEmail(email)
		u.setDepartment(homeDepartment.name)
		u.setDepartmentCode(homeDepartment.code)
		u.setFoundUser(true)
		u
	}
	
	def toStringProps = Seq(
		"universityId" -> universityId,
		"userId" -> userId,
		"name" -> (firstName + " " + lastName),
		"email" -> email)

}

trait MemberProperties {
	@Id @BeanProperty var universityId: String = _
	@BeanProperty @Column(nullable = false) var userId: String = _
	@BeanProperty var firstName: String = _
	@BeanProperty var lastName: String = _
	@BeanProperty var email: String = _
	
	@BeanProperty var title: String = _
	@BeanProperty var fullFirstName: String = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MemberUserTypeUserType")
	@BeanProperty var userType: MemberUserType = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.GenderUserType")
	@BeanProperty var gender: Gender = _
	
	@BeanProperty var nationality: String = _
	@BeanProperty var homeEmail: String = _
	@BeanProperty var mobileNumber: String = _
	
	@OneToOne
	@JoinColumn(name="PHOTO_ID")
	@BeanProperty var photo: FileAttachment = _
	
	@BeanProperty var inUseFlag: String = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var inactivationDate: LocalDate = _
	
	@BeanProperty var groupName: String = _
	
	@ManyToOne
	@JoinColumn(name = "home_department_id")
	@BeanProperty var homeDepartment: Department = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var dateOfBirth: LocalDate = _
}

trait StudentProperties {
	@BeanProperty var sprCode: String = _
	@BeanProperty var sitsCourseCode: String = _
	
	@ManyToOne
	@JoinColumn(name = "route_id")
	@BeanProperty var route: Route = _
	
	@BeanProperty var yearOfStudy: JInteger = _
	@BeanProperty var attendanceMode: String = _
	
	@BeanProperty var studentStatus: String = _
	
	@BeanProperty var fundingSource: String = _
	@BeanProperty var programmeOfStudy: String = _
	
	@BeanProperty var intendedAward: String = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var academicYear: AcademicYear = _
	
	@ManyToOne
	@JoinColumn(name = "study_department_id")
	@BeanProperty var studyDepartment: Department = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var courseStartYear: AcademicYear = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var yearCommencedDegree: AcademicYear = _
	
	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@BeanProperty var courseBaseYear: AcademicYear = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var courseEndDate: LocalDate = _
	
	@BeanProperty var transferReason: String = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var beginDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var endDate: LocalDate = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	@BeanProperty var expectedEndDate: LocalDate = _
	
	@BeanProperty var feeStatus: String = _
	@BeanProperty var domicile: String = _
	@BeanProperty var highestQualificationOnEntry: String = _
	
	@BeanProperty var lastInstitute: String = _
	@BeanProperty var lastSchool: String = _	
	
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="HOME_ADDRESS_ID")
	@BeanProperty var homeAddress: Address = null
	
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name="TERMTIME_ADDRESS_ID")
	@BeanProperty var termtimeAddress: Address = null

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = Array(ALL))
	@BeanProperty var nextOfKins:JList[NextOfKin] = ArrayList()
}

trait StaffProperties {
	@BeanProperty var teachingStaff: JBoolean = _
}

trait AlumniProperties {
	
}