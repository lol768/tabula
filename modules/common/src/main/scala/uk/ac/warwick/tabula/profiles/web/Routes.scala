package uk.ac.warwick.tabula.profiles.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/profiles"

	def home = context + "/"
	def search = context + "/search"

	object oldProfile {
		def view(member: Member) = context + "/view/%s" format encoded(member.universityId)
		def view(member: Member, meeting: AbstractMeetingRecord) = context + "/view/%s?meeting=%s" format (encoded(member.universityId), encoded(meeting.id))
		def view(scyd: StudentCourseYearDetails, meeting: AbstractMeetingRecord) = context + "/view/course/%s/%s?meeting=%s" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString), encoded(meeting.id))
		def photo(member: Member) = context + "/view/photo/%s.jpg" format encoded(member.universityId)
		def mine = context + "/view/me"

		def viewTimetable(member: Member) = context + "/timetable/%s" format encoded(member.universityId)
	}

	object Profile {
		def identity(member: Member) =
			context + "/view/%s" format encoded(member.universityId)
		def identity(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def timetable(member: Member) = context + "/view/%s/timetable" format encoded(member.universityId)
		def timetable(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/timetable" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def relationshipType(member: Member, relationshipType: StudentRelationshipType) =
			context + "/view/%s/%s" format (encoded(member.universityId), encoded(relationshipType.urlPart))
		def relationshipType(scyd: StudentCourseYearDetails, relationshipType: StudentRelationshipType) =
			context + "/view/course/%s/%s/%s" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString), encoded(relationshipType.urlPart))
		def relationshipType(scd: StudentCourseDetails, academicYear: AcademicYear, relationshipType: StudentRelationshipType) =
			context + "/view/course/%s/%s/%s" format (encoded(scd.urlSafeId), encoded(academicYear.value.toString), encoded(relationshipType.urlPart))
		def assignments(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/assignments" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def modules(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/modules" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def seminars(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/seminars" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def marking(member: Member) = context + "/view/%s/marking" format encoded(member.universityId)
		def marking(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/marking" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))
		def attendance(member: Member) = context + "/view/%s/attendance" format encoded(member.universityId)
		def attendance(scyd: StudentCourseYearDetails) =
			context + "/view/course/%s/%s/attendance" format (encoded(scyd.studentCourseDetails.urlSafeId), encoded(scyd.academicYear.value.toString))

	}

	def students(relationshipType: StudentRelationshipType) = context + "/%s/students" format encoded(relationshipType.urlPart)

	object relationships {
		def apply(department: Department, relationshipType: StudentRelationshipType) =
			context + "/department/%s/%s" format (encoded(department.code), encoded(relationshipType.urlPart))
		def missing(department: Department, relationshipType: StudentRelationshipType) =
			context + "/department/%s/%s/missing" format (encoded(department.code), encoded(relationshipType.urlPart))
		def allocate(department: Department, relationshipType: StudentRelationshipType) =
			context + "/department/%s/%s/allocate" format (encoded(department.code), encoded(relationshipType.urlPart))
		def template(department: Department, relationshipType: StudentRelationshipType) =
			context + "/department/%s/%s/template" format (encoded(department.code), encoded(relationshipType.urlPart))
	}

	object admin {
		def apply(department: Department) = Routes.home // TODO https://repo.elab.warwick.ac.uk/projects/TAB/repos/tabula/pull-requests/145/overview?commentId=1012
		def departmentPermissions(department: Department) = context + "/admin/department/%s/permissions" format encoded(department.code)
	}

	object scheduledMeeting {
		def confirm(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType) =
			context + "/%s/meeting/%s/schedule/%s/confirm" format(
				encoded(relationshipType.urlPart),
				encoded(studentCourseDetails.urlSafeId),
				encoded(meetingRecord.id)
			)

		def reschedule(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType) =
			context + "/%s/meeting/%s/schedule/%s/edit" format(
				encoded(relationshipType.urlPart),
				encoded(studentCourseDetails.urlSafeId),
				encoded(meetingRecord.id)
			)

		def missed(meetingRecord: ScheduledMeetingRecord, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType) =
			context + "/%s/meeting/%s/missed" format(encoded(relationshipType.urlPart), encoded(meetingRecord.id))
	}

	object Note {
		def apply(student: StudentMember, point: AttendanceMonitoringPoint) =
			context + "/attendance/note/%s/%s" format(encoded(student.universityId), encoded(point.id))

	}
}
