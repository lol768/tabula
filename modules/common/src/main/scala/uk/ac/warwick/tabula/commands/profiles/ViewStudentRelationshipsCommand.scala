package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}

import scala.collection.immutable.TreeMap

// wrapper class for relationship data - just for less crufty method signature
case class RelationshipGraph(
	studentMap: TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]],
	studentCount: Int,
	missingCount: Int,
	courseMap: Map[StudentCourseDetails, Course],
	yearOfStudyMap: Map[StudentCourseDetails, Int]
)
case class SortableAgentIdentifier(agentId:String, lastName:Option[String]){
	 val sortkey = lastName.getOrElse("") + agentId
	 override def toString() = sortkey
}
object SortableAgentIdentifier{
	def apply(r:StudentRelationship) = new SortableAgentIdentifier(r.agent, r.agentMember.map(_.lastName))

	val KeyOrdering = Ordering.by { a:SortableAgentIdentifier => a.sortkey }
}

class ViewStudentRelationshipsCommand(val department: Department, val relationshipType: StudentRelationshipType)
	extends Command[RelationshipGraph] with Unaudited with ReadOnly {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var relationshipService = Wire.auto[RelationshipService]
	var profileService = Wire.auto[ProfileService]


	override def applyInternal(): RelationshipGraph = transactional(readOnly = true) {
		// get all agent/student relationships by dept

		// all students in department X
		val unsortedAgentRelationshipsByStudentDept = relationshipService.listStudentRelationshipsByDepartment(relationshipType, department)

		// all students with a tutor in department X
		val unsortedAgentRelationshipsByStaffDept = relationshipService.listStudentRelationshipsByStaffDepartment(relationshipType, department)

		// combine the two and remove the dups
		val unsortedAgentRelationships = (unsortedAgentRelationshipsByStudentDept ++ unsortedAgentRelationshipsByStaffDept)
				// TAB-2750 treat relationships between the same agent and student COURSE as identical
				.groupBy { rel => (rel.agent, rel.studentCourseDetails) }
				.map { case (_, rels) => rels.maxBy { rel => rel.startDate.getMillis } }
				.toSeq

		// group into map by agent lastname, or id if the lastname is unavailable
		val groupedAgentRelationships = unsortedAgentRelationships.groupBy(r=>SortableAgentIdentifier(r))

		//  alpha sort by constructing a TreeMap
		val sortedAgentRelationships = TreeMap(groupedAgentRelationships.toSeq:_*)(SortableAgentIdentifier.KeyOrdering)

		// count students
		val studentsInDepartmentCount = profileService.countStudentsByDepartment(department)
		val departmentStudentsWithoutAgentCount = relationshipService.listStudentsWithoutRelationship(relationshipType, department).distinct.size

		val studentIdsInDepartment = unsortedAgentRelationshipsByStudentDept.map(_.studentId).distinct
		val studentsOutsideDepartmentCount =
			unsortedAgentRelationshipsByStaffDept.map(_.studentId).distinct
				.filterNot(id=>studentIdsInDepartment.contains(id)).size

		val courseMap: Map[StudentCourseDetails, Course] = benchmarkTask("courseDetails") {
			relationshipService.coursesForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		val yearOfStudyMap: Map[StudentCourseDetails, Int] = benchmarkTask("yearsOfStudy") {
			relationshipService.latestYearsOfStudyForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		RelationshipGraph(
			sortedAgentRelationships,
			studentsInDepartmentCount + studentsOutsideDepartmentCount,
			departmentStudentsWithoutAgentCount,
			courseMap,
			yearOfStudyMap
		)
	}
}

class MissingStudentRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType)
	extends Command[(Int, Seq[Member])] with Unaudited with ReadOnly {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]

	override def applyInternal(): (Int, Seq[Member]) = transactional(readOnly = true) {
		val studentCount = profileService.countStudentsByDepartment(department)
		studentCount match {
			case 0 => (0, Nil)
			case c => (c, relationshipService.listStudentsWithoutRelationship(relationshipType, department))
		}
	}
}