package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceServiceComponent, AutowiringAttendanceMonitoringEventAttendanceServiceComponent}

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupEventAttendance}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.{FeaturesComponent, AutowiringFeaturesComponent, CurrentUser, ItemNotFoundException}
import org.joda.time.DateTime
import RecordAttendanceCommand._

object RecordAttendanceCommand {
	type UniversityId = String
	
	def apply(event: SmallGroupEvent, week: Int, user: CurrentUser) =
		new RecordAttendanceCommand(event, week, user)
			with ComposableCommand[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])]
			with SmallGroupEventInFutureCheck
			with RecordAttendanceCommandPermissions
			with RecordAttendanceDescription
			with RecordAttendanceCommandValidation
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringMonitoringPointGroupProfileServiceComponent
			with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
			with AutowiringFeaturesComponent {
		override lazy val eventName = "RecordAttendance"
	}
}

trait AddAdditionalStudent {
	self: SmallGroupServiceComponent with RecordAttendanceState =>
	def occurrence: SmallGroupEventOccurrence

	var additionalStudent: Member = _
	var replacedWeek: JInteger = _
	var replacedEvent: SmallGroupEvent = _

	lazy val manuallyAddedUniversityIds = occurrence.attendance.asScala.filter { _.addedManually }.map { _.universityId }

	var linkedAttendance: SmallGroupEventAttendance = _

	def addAdditionalStudent(members: Seq[MemberOrUser]) {
		Option(additionalStudent)
			.filterNot { member => members.find { _.universityId == member.universityId }.isDefined }
			.foreach { member =>
				val attendance = transactional() {
					smallGroupService.saveOrUpdateAttendance(member.universityId, event, week, AttendanceState.NotRecorded, user)
				}

				attendance.addedManually = true
				Option(replacedEvent).foreach { event =>
					val replacedOccurrence = transactional() { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, replacedWeek) }
					val replacedAttendance = transactional() {
						smallGroupService.getAttendance(member.universityId, replacedOccurrence) match {
							case Some(attendance) if attendance.state == AttendanceState.Attended => attendance
							case Some(attendance) => {
								attendance.state = AttendanceState.MissedAuthorised
								smallGroupService.saveOrUpdate(attendance)
								attendance
							}
							case None => smallGroupService.saveOrUpdateAttendance(member.universityId, replacedEvent, replacedWeek, AttendanceState.MissedAuthorised, user)
						}
					}

					attendance.replacesAttendance = replacedAttendance
				}

				linkedAttendance = transactional() { smallGroupService.saveOrUpdate(attendance); attendance }

				studentsState.put(member.universityId, null)
			}
	}
}

trait RemoveAdditionalStudent {
	self: SmallGroupServiceComponent with RecordAttendanceState =>

	var removeAdditionalStudent: Member = _

	def doRemoveAdditionalStudent(members: Seq[MemberOrUser]) {
		Option(removeAdditionalStudent)
			.filter { member => members.find { _.universityId == member.universityId }.isDefined }
			.foreach { member =>
				transactional() {
					smallGroupService.deleteAttendance(member.universityId, event, week, true)
				}

				studentsState.remove(member.universityId)
			}
	}
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int, val user: CurrentUser) 
	extends CommandInternal[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] 
		with RecordAttendanceState
		with AddAdditionalStudent
		with RemoveAdditionalStudent
		with PopulateOnForm
		with TaskBenchmarking {

	self: SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent with FeaturesComponent
		with MonitoringPointGroupProfileServiceComponent with AttendanceMonitoringEventAttendanceServiceComponent =>
		
	if (!event.group.groupSet.collectAttendance) throw new ItemNotFoundException

	lazy val occurrence = transactional() { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week) }

	var studentsState: JMap[UniversityId, AttendanceState] =
		LazyMaps.create { member: UniversityId => null: AttendanceState }.asJava
	
	lazy val members: Seq[MemberOrUser] = {
		(event.group.students.users.map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			(false, MemberOrUser(member, user))
		} ++ occurrence.attendance.asScala.toSeq.map { a =>
			val member = profileService.getMemberByUniversityId(a.universityId)
			val user = userLookup.getUserByWarwickUniId(a.universityId)
			(a.addedManually, MemberOrUser(member, user))
		}).distinct.sortBy { case (addedManually, mou) => (!addedManually, mou.lastName, mou.firstName, mou.universityId) }
			.map { case (_, mou) => mou }
	}

	lazy val attendanceNotes: Map[MemberOrUser, Map[SmallGroupEventOccurrence, SmallGroupEventAttendanceNote]] = benchmarkTask("Get attendance notes") {
		smallGroupService.findAttendanceNotes(members.map(_.universityId), Seq(occurrence)).groupBy(_.student).map{
			case (student, noteMap) => MemberOrUser(student) -> noteMap.groupBy(_.occurrence).map{
				case(o, notes) => o -> notes.head
			}
		}.toMap
	}

	lazy val attendances: Map[MemberOrUser, Option[SmallGroupEventAttendance]] = benchmarkTask("Get attendances") {
		val all = occurrence.attendance.asScala
		members.map { m => (m, all.find { a => a.universityId == m.universityId })}.toMap
	}
	
	def populate() {
		studentsState = members.map { member =>
			member.universityId -> 
				occurrence.attendance.asScala
					.find { _.universityId == member.universityId }
					.flatMap { a => Option(a.state) }.orNull
		}.toMap.asJava
	}

	def applyInternal() = {
		val attendances = studentsState.asScala.flatMap { case (studentId, state) =>
			println((studentId, state))
			if (state == null) {
				smallGroupService.deleteAttendance(studentId, event, week)
				None
			} else {
				Some(smallGroupService.saveOrUpdateAttendance(studentId, event, week, state, user))
			}
		}.toSeq

		monitoringPointGroupProfileService.updateCheckpointsForAttendance(attendances)
		if (features.attendanceMonitoringAcademicYear2014)
			attendanceMonitoringEventAttendanceService.updateCheckpoints(attendances)

		(occurrence, attendances)
	}
}

trait RecordAttendanceCommandValidation extends SelfValidating {
	self: RecordAttendanceState with UserLookupComponent with SmallGroupEventInFutureCheck =>
	
	def validate(errors: Errors) {
		val invalidUsers: Seq[UniversityId] = studentsState.asScala.map {
			case (studentId, _) => studentId
		}.filter(s => !userLookup.getUserByWarwickUniId(s).isFoundUser).toSeq

		if (invalidUsers.length > 0) {
			errors.rejectValue("studentsState", "smallGroup.attendees.invalid", Array(invalidUsers), "")
		}
		
		// TAB-1791 Allow attendance to be recorded for users not in the group, they were in the group in the past or submitting would be a pain
		/*else {
			val missingUsers: Seq[UniversityId] = studentsState.asScala.map {
				case (studentId, _) => studentId
			}.filter(s => event.group.students.users.filter(u => u.getWarwickId() == s).length == 0).toSeq
			if (missingUsers.length > 0) {
				errors.rejectValue("studentsState", "smallGroup.attendees.missing", Array(missingUsers), "")
			}
		}*/
		
		studentsState.asScala.foreach { case (studentId, state) => 
			errors.pushNestedPath(s"studentsState[$studentId]")
			
			if (isFutureEvent && !(state == null || state == AttendanceState.MissedAuthorised || state == AttendanceState.NotRecorded)) {
				errors.rejectValue("", "smallGroup.attendance.beforeEvent")
			}
			
			errors.popNestedPath()
		}
	}
	
}

trait RecordAttendanceCommandPermissions extends RequiresPermissionsChecking {
	self: RecordAttendanceState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, event)
	}
}

trait RecordAttendanceState {
	val event: SmallGroupEvent
	val week: Int
	val user: CurrentUser
	
	def studentsState: JMap[UniversityId, AttendanceState]
	def members: Seq[MemberOrUser]
}

trait SmallGroupEventInFutureCheck {
	self: RecordAttendanceState with TermServiceComponent =>
	
	lazy val isFutureEvent = {
		val academicYear = event.group.groupSet.academicYear
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		
		currentAcademicWeek < week
	}
}

trait RecordAttendanceDescription extends Describable[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])] {
	this: RecordAttendanceState =>
	def describe(d: Description) {
		d.smallGroupEvent(event)
		d.property("week", week)
	}
}
