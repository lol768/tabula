package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, SmallGroupDaoComponent, Daoisms}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._

trait SmallGroupServiceComponent {
	def smallGroupService: SmallGroupService
}

trait AutowiringSmallGroupServiceComponent extends SmallGroupServiceComponent {
	var smallGroupService = Wire[SmallGroupService]
}

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent]
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup]
	def findSmallGroupsByStudent(student: User): Seq[SmallGroup]
	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet]

	def updateAttendance(smallGroupEvent: SmallGroupEvent, weekNumber: Int, universityIds: Seq[String]): SmallGroupEventOccurrence
	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String]
}

abstract class AbstractSmallGroupService extends SmallGroupService {
	self: SmallGroupDaoComponent with SmallGroupMembershipHelpers=>

	def getSmallGroupSetById(id: String) = smallGroupDao.getSmallGroupSetById(id)
	def getSmallGroupById(id: String) = smallGroupDao.getSmallGroupById(id)
	def getSmallGroupEventById(id: String) = smallGroupDao.getSmallGroupEventById(id)
	def getSmallGroupEventOccurrenceById(id: String) = smallGroupDao.getSmallGroupEventOccurrenceById(id)

	def saveOrUpdate(smallGroupSet: SmallGroupSet) = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = smallGroupDao.saveOrUpdate(smallGroupEvent)

	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent] = eventTutorsHelper.findBy(user)
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup] = groupTutorsHelper.findBy(user)

	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet] = groupSetMembersHelper.findBy(user)

	def findSmallGroupsByStudent(user: User): Seq[SmallGroup] = studentGroupHelper.findBy(user)
	
	def getAttendees(event: SmallGroupEvent, weekNumber: Int): JList[String] = 
		smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) match {
			case Some(occurrence) => occurrence.attendees.includeUsers
			case _ => JArrayList()
		}

	def updateAttendance(event: SmallGroupEvent, weekNumber: Int, universityIds: Seq[String]): SmallGroupEventOccurrence = {
		val occurrence = smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber) getOrElse {
			val newOccurrence = new SmallGroupEventOccurrence()
			newOccurrence.event = event
			newOccurrence.week = weekNumber
			smallGroupDao.saveOrUpdate(newOccurrence)
			newOccurrence
		}

		occurrence.attendees.includeUsers.clear()
		occurrence.attendees.includeUsers.addAll(universityIds.asJava)
		occurrence
	}
}

trait SmallGroupMembershipHelpers {
	val eventTutorsHelper:UserGroupMembershipHelper[SmallGroupEvent]
  //TODO can this be removed? findSmallGroupsByTutor could just call findSmallGroupEventsByTutor and then group by group
	val groupTutorsHelper:UserGroupMembershipHelper[SmallGroup]
	val groupSetMembersHelper:UserGroupMembershipHelper[SmallGroupSet]
	val studentGroupHelper: UserGroupMembershipHelper[SmallGroup]
}

// new up UGMHs which will Wire.auto() their dependencies
trait SmallGroupMembershipHelpersImpl extends SmallGroupMembershipHelpers {
	val eventTutorsHelper = new UserGroupMembershipHelper[SmallGroupEvent]("tutors")
	val groupTutorsHelper = new UserGroupMembershipHelper[SmallGroup]("events.tutors")
	val groupSetMembersHelper = new UserGroupMembershipHelper[SmallGroupSet]("members")
	val studentGroupHelper = new UserGroupMembershipHelper[SmallGroup]("students")
}

@Service("smallGroupService")
class SmallGroupServiceImpl 
	extends AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent
	  with SmallGroupMembershipHelpersImpl
		with Logging