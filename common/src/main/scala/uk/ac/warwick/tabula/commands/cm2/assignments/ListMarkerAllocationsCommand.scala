package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import ListMarkerAllocationsCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, CM2MarkingWorkflowServiceComponent, UserLookupComponent, _}

import scala.collection.immutable.SortedSet
import scala.collection.mutable


case class MarkerAllocations(
	allStudents: SortedSet[Student],
	keys: List[String], // ordered keys for all of the other maps - getting freemarker to iterate across maps in order is dumb
	unallocatedStudents: Map[String, SortedSet[Student]],
	allocations: Map[String, Map[Marker, SortedSet[Student]]],
	markers: Map[String, SortedSet[Marker]],
	allocateByStage: Map[String, Boolean]
)

trait FetchMarkerAllocations {
	self: UserLookupComponent with CM2MarkingWorkflowServiceComponent
		with AssessmentMembershipServiceComponent =>

	def fetchAllocations(assignment: Assignment): MarkerAllocations = {
		val workflow = assignment.cm2MarkingWorkflow
		val stagesByRole = workflow.allStages.groupBy(_.roleName)
		val allStudents = SortedSet(assessmentMembershipService.determineMembershipUsers(assignment): _*)

		val allocations = mutable.Map[String, Map[Marker, SortedSet[Student]]]()
		val markers = mutable.Map[String, SortedSet[Marker]]()
		val allocateByStage = mutable.Map[String, Boolean]()

		def toSortedSet(allocations: Allocations) = allocations.map{case(marker, students) =>
			marker -> SortedSet(students.toSeq: _*)
		}

		for {
			(role, stages) <- stagesByRole
			stage <- stages
		} if(workflow.workflowType.rolesShareAllocations) {
			// key by role as the allocations are the same
			allocations += role -> toSortedSet(cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage))
			markers += role -> SortedSet(workflow.markers(stage): _*)
			allocateByStage += role -> false
		} else {
			allocations += stage.allocationName -> toSortedSet(cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage))
			markers += stage.allocationName -> SortedSet(workflow.markers(stage): _*)
			allocateByStage += stage.allocationName -> stage.stageAllocation
		}

		val unallocatedStudents = allocations.map{ case(key, allocation) =>
			key ->  (allStudents -- allocation.filterKeys(_.isFoundUser).values.flatten.toSet)
		}

		MarkerAllocations(
			allStudents = allStudents,
			keys = workflow.allocationOrder,
			unallocatedStudents = unallocatedStudents.toMap,
			allocations = allocations.toMap,
			markers = markers.toMap,
			allocateByStage = allocateByStage.toMap
		)
	}
}

object ListMarkerAllocationsCommand {

	type Student = User
	type Marker = User

	implicit val userOrdering: Ordering[User] = Ordering.by { u: User => (u.getLastName, u.getFirstName) }

	def apply(assignment: Assignment) = new ListMarkerAllocationsCommandInternal(assignment)
		with ComposableCommand[MarkerAllocations]
		with ListMarkerAllocationsPermissions
		with Unaudited
		with ReadOnly
		with AutowiringUserLookupComponent
		with AutowiringCM2MarkingWorkflowServiceComponent
		with AutowiringAssessmentMembershipServiceComponent
}

class ListMarkerAllocationsCommandInternal(val assignment: Assignment) extends CommandInternal[MarkerAllocations]
	with ListMarkerAllocationsState with FetchMarkerAllocations {

	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent =>

	def applyInternal(): MarkerAllocations = fetchAllocations(assignment)
}

trait ListMarkerAllocationsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListMarkerAllocationsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}

trait ListMarkerAllocationsState {
	val assignment: Assignment
}