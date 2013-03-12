package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.{Projections, Restrictions, Order}
import uk.ac.warwick.tabula.helpers.{ FoundUser, Logging }
import uk.ac.warwick.tabula.services._

/**
 * Service providing access to Assignments and related objects.
 *
 * TODO this is getting a bit monstrous and all-encompassing.
 */
trait AssignmentService {
	def getAssignmentById(id: String): Option[Assignment]
	def save(assignment: Assignment)

	def saveSubmission(submission: Submission)
	def getSubmissionByUniId(assignment: Assignment, uniId: String): Option[Submission]
	def getSubmission(id: String): Option[Submission]
	
	def getExtensionById(id: String): Option[Extension]

	def delete(group: AssessmentGroup): Unit

	def delete(submission: Submission): Unit

	def deleteFormField(field: FormField) : Unit

	def deleteOriginalityReport(attachment: FileAttachment): Unit
	def saveOriginalityReport(attachment: FileAttachment): Unit
	
	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]

	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]]

	def getEnrolledAssignments(user: User): Seq[Assignment]
	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def save(group: AssessmentGroup): Unit
	def getAssessmentGroup(id: String): Option[AssessmentGroup]
	def getAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]

	def getUpstreamAssignment(id: String): Option[UpstreamAssignment]

	/**
	 * Get all UpstreamAssignments that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getAssessmentGroups.
	 */
	def getUpstreamAssignments(module: Module): Seq[UpstreamAssignment]
	def getUpstreamAssignments(department: Department): Seq[UpstreamAssignment]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

	def save(assignment: UpstreamAssignment): UpstreamAssignment
	def save(group: UpstreamAssessmentGroup)
	def replaceMembers(group: UpstreamAssessmentGroup, universityIds: Seq[String])

	def determineMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[MembershipItem]
	def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[User]
	def determineMembershipUsers(assignment: Assignment): Seq[User]

	def isStudentMember(user: User, upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Boolean

	def getStudentFeedback(assignment: Assignment, warwickId: String): Option[Feedback]
	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int
}

@Service(value = "assignmentService")
class AssignmentServiceImpl extends AssignmentService with AssignmentMembershipMethods with Daoisms with Logging {
	import Restrictions._

	@Autowired var userLookup: UserLookupService = _
	@Autowired var auditEventIndexService: AuditEventIndexService = _

	def getAssignmentById(id: String) = getById[Assignment](id)
	def save(assignment: Assignment) = session.saveOrUpdate(assignment)
	def saveSubmission(submission: Submission) = {
		session.saveOrUpdate(submission)
		session.flush()
	}

	def replaceMembers(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
		if (debugEnabled) debugReplace(template, universityIds)
		getAssessmentGroup(template).map { group =>
			val collection = group.members.staticIncludeUsers
			collection.clear
			collection.addAll(universityIds)
		} getOrElse {
			logger.warn("No such assessment group found: " + template.toText)
		}
	}

	/**
	 * Tries to find an identical UpstreamAssignment in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment: UpstreamAssignment): Option[UpstreamAssignment] = session.newCriteria[UpstreamAssignment]
		.add(Restrictions.eq("moduleCode", assignment.moduleCode))
		.add(Restrictions.eq("sequence", assignment.sequence))
		.uniqueResult

	def save(group:AssessmentGroup) = session.saveOrUpdate(group)

	def save(assignment: UpstreamAssignment): UpstreamAssignment =
		find(assignment)
			.map { existing =>
				if (existing needsUpdatingFrom assignment) {
					existing.copyFrom(assignment)
					session.update(existing)
				}
				
				existing
			}
			.getOrElse { session.save(assignment); assignment }

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
		.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
		.add(Restrictions.eq("academicYear", group.academicYear))
		.add(Restrictions.eq("moduleCode", group.moduleCode))
		.add(Restrictions.eq("occurrence", group.occurrence))
		.uniqueResult

	def save(group: UpstreamAssessmentGroup) =
		find(group)
			.map { existing =>
				// do nothing. nothing else to update except members
				//session.update(existing.id, group)
			}
			.getOrElse { session.save(group) }

	def getSubmissionByUniId(assignment: Assignment, uniId: String) = {
		session.newCriteria[Submission]
			.add(Restrictions.eq("assignment", assignment))
			.add(Restrictions.eq("universityId", uniId))
			.uniqueResult
	}

	def getSubmission(id: String) = getById[Submission](id)

	def delete(submission: Submission) {
		submission.assignment.submissions.remove(submission)
		session.delete(submission)
		// force delete now, just for the cases where we re-insert in the same session
		// (i.e. when a student is resubmitting work). [HFC-385#comments]
		session.flush()
	}

	def getExtensionById(id: String) = getById[Extension](id)

	def deleteFormField(field: FormField) {
		session.delete(field)
	}

	/**
	 * Deletes the OriginalityReport attached to this Submission if one
	 * exists. It flushes the session straight away because otherwise deletes
	 * don't always happen until after some insert operation that assumes
	 * we've deleted it.
	 */
	def deleteOriginalityReport(attachment: FileAttachment) {
		if (attachment.originalityReport != null) {
			val report = attachment.originalityReport
			attachment.originalityReport = null
			session.delete(report)
			session.flush()
		}
	}

	def saveOriginalityReport(attachment: FileAttachment) {
		attachment.originalityReport.attachment = attachment
		session.save(attachment.originalityReport)
	}

	def getEnrolledAssignments(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a 
				from Assignment a 
				left join fetch a.assessmentGroups ag
				where 
					(1 = (
						select 1 from uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup uag
						where uag.moduleCode = ag.upstreamAssignment.moduleCode
							and uag.assessmentGroup = ag.upstreamAssignment.assessmentGroup
							and uag.academicYear = a.academicYear
							and uag.occurrence = ag.occurrence
							and :universityId in elements(uag.members.staticIncludeUsers)
					) or :userId in elements(a.members.includeUsers))
					and :userId not in elements(a.members.excludeUsers)
					and a.deleted = false and a.archived = false
		""").setString("universityId", user.getWarwickId()).setString("userId", user.getUserId()).seq

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a from Assignment a
				join a.feedbacks as f
				where f.universityId = :universityId
				and f.released=true""")
			.setString("universityId", universityId)
			.seq

	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a from Assignment a
				join a.submissions as f
				where f.universityId = :universityId""")
			.setString("universityId", universityId)
			.seq

	def getAssignmentWhereMarker(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a 
				from Assignment a
				where (:userId in elements(a.markingWorkflow.firstMarkers.includeUsers)
					or :userId in elements(a.markingWorkflow.secondMarkers.includeUsers))
					and a.deleted = false and a.archived = false
		""").setString("userId", user.getUserId()).seq

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module) =
		session.newQuery[Assignment]("from Assignment where name=:name and academicYear=:year and module=:module and deleted=0")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.seq

	/* get users whose feedback is not published and who have not submitted work suspected
	 * of being plagiarised */
	def getUsersForFeedback(assignment: Assignment): Seq[Pair[String, User]] = {
		//val uniIds = assignment.unreleasedFeedback.map { _.universityId }
		//uniIds.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }

		val plagiarisedSubmissions = assignment.submissions.filter { submission => submission.suspectPlagiarised }
		val plagiarisedIds = plagiarisedSubmissions.map { _.universityId }
		val unreleasedIds = assignment.unreleasedFeedback.map { _.universityId }
		val unplagiarisedUnreleasedIds = unreleasedIds.filter { uniId => !plagiarisedIds.contains(uniId) }
		unplagiarisedUnreleasedIds.map { (id) => (id, userLookup.getUserByWarwickUniId(id, false)) }
	}

	def recentAssignment(department: Department) = {
		//auditEventIndexService.recentAssignment(department)
		session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(Restrictions.eq("m.department", department))
			.add(Restrictions.isNotNull("createdDate"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(1)
			.uniqueResult
	}

	def getAssignmentsByName(partialName: String, department: Department) = {
		session.newCriteria[Assignment]
			.createAlias("module", "mod")
			.add(Restrictions.eq("mod.department", department))
			.add(Restrictions.ilike("name", "%" + partialName + "%"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(15)
			.list
	}

	def getAssessmentGroup(id:String) = getById[AssessmentGroup](id)
	def getAssessmentGroup(template: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = find(template)

	def delete(group: AssessmentGroup) {
		group.assignment.assessmentGroups.remove(group)
		session.delete(group)
		session.flush()
	}

	def getUpstreamAssignment(id: String) = getById[UpstreamAssignment](id)

	def getUpstreamAssignments(module: Module) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.addOrder(Order.asc("sequence"))
			.list filter isInteresting
	}

	def getUpstreamAssignments(department: Department) = {
		session.newCriteria[UpstreamAssignment]
			.add(Restrictions.eq("departmentCode", department.code.toUpperCase))
			.addOrder(Order.asc("moduleCode"))
			.addOrder(Order.asc("sequence"))
			.list filter isInteresting
	}

	def getStudentFeedback(assignment: Assignment, uniId: String) = {
		assignment.findFullFeedback(uniId)
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def countFullFeedback(assignment: Assignment): Int = {
		session.createQuery("""select count(*) from Feedback f join f.attachments a
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
			.setEntity("assignment", assignment)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	private def isInteresting(assignment: UpstreamAssignment) = {
		!(assignment.name contains "NOT IN USE")
	}

	def getAssessmentGroups(upstreamAssignment: UpstreamAssignment, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("academicYear", academicYear))
			.add(Restrictions.eq("moduleCode", upstreamAssignment.moduleCode))
			.add(Restrictions.eq("assessmentGroup", upstreamAssignment.assessmentGroup))
			.list
	}

	private def debugReplace(template: UpstreamAssessmentGroup, universityIds: Seq[String]) {
		logger.debug("Setting %d members in group %s" format (universityIds.size, template.toText))
	}
}

trait AssignmentMembershipMethods { self: AssignmentServiceImpl =>

	def determineMembership(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[MembershipItem] = {

		val sitsUsers = upstream flatMap { upstream =>
			upstream.members.members map { id =>
				id -> userLookup.getUserByWarwickUniId(id)
			}
		}

		val includes = others map { _.includeUsers map { id => id -> userLookup.getUserByUserId(id) } } getOrElse Nil
		val excludes = others map { _.excludeUsers map { id => id -> userLookup.getUserByUserId(id) } } getOrElse Nil

		// convert lists of Users to lists of MembershipItems that we can render neatly together.

		val includeItems = makeIncludeItems(includes, sitsUsers)
		val excludeItems = makeExcludeItems(excludes, sitsUsers)
		val sitsItems = makeSitsItems(includes, excludes, sitsUsers)

		includeItems ++ excludeItems ++ sitsItems
	}

	/**
	 * Returns just a list of User objects who are on this assessment group.
	 */
	def determineMembershipUsers(upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Seq[User] = {
		determineMembership(upstream, others) filter notExclude map toUser filter notNull
	}

	/**
	 * Returns a simple list of User objects for students who are enrolled on this assignment. May be empty.
	 */
	def determineMembershipUsers(assignment: Assignment): Seq[User] = {
		determineMembershipUsers(assignment.upstreamAssessmentGroups, Option(assignment.members))
	}

	def isStudentMember(user: User, upstream: Seq[UpstreamAssessmentGroup], others: Option[UserGroup]): Boolean = {
		if (others map {_.excludeUsers contains user.getUserId } getOrElse false) false
		else if (others map { _.includeUsers contains user.getUserId } getOrElse false) true
		else upstream exists {
			_.members.staticIncludeUsers contains user.getWarwickId //Yes, definitely Uni ID when checking SITS group
		}
	}

	private def sameUserIdAs(user: User) = (other: Pair[String, User]) => { user.getUserId == other._2.getUserId }
	private def in(seq: Seq[Pair[String, User]]) = (other: Pair[String, User]) => { seq exists sameUserIdAs(other._2) }

	private def makeIncludeItems(includes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		includes map {
			case (id, user) =>
				val extraneous = sitsUsers exists sameUserIdAs(user)
				MembershipItem(
					user = user,
					universityId = universityId(user, None),
					userId = userId(user, Some(id)),
					itemType = "include",
					extraneous = extraneous)
		}

	private def makeExcludeItems(excludes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		excludes map {
			case (id, user) =>
				val extraneous = !(sitsUsers exists sameUserIdAs(user))
				MembershipItem(
					user = user,
					universityId = universityId(user, None),
					userId = userId(user, Some(id)),
					itemType = "exclude",
					extraneous = extraneous)
		}

	private def makeSitsItems(includes: Seq[Pair[String, User]], excludes: Seq[Pair[String, User]], sitsUsers: Seq[Pair[String, User]]) =
		sitsUsers filterNot in(includes) filterNot in(excludes) map {
			case (id, user) =>
				MembershipItem(
					user = user,
					universityId = universityId(user, Some(id)),
					userId = userId(user, None),
					itemType = "sits",
					extraneous = false)
		}

	private def universityId(user: User, fallback: Option[String]) = option(user) map { _.getWarwickId } orElse fallback
	private def userId(user: User, fallback: Option[String]) = option(user) map { _.getUserId } orElse fallback

	private def option(user: User): Option[User] = user match {
		case FoundUser(u) => Some(user)
		case _ => None
	}

	private def toUser(item: MembershipItem) = item.user
	private def notExclude(item: MembershipItem) = item.itemType != "exclude"
	private def notNull[A](any: A) = { any != null }
}

/** Item in list of members for displaying in view. */
case class MembershipItem(
	user: User,
	universityId: Option[String],
	userId: Option[String],
	itemType: String, // sits, include or exclude
	/**
	 * If include type, this item adds a user who's already in SITS.
	 * If exclude type, this item excludes a user who isn't in the list anyway.
	 */
	extraneous: Boolean)
