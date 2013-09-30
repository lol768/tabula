package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.util.termdates.{Term, TermFactoryImpl}
import org.joda.time.base.BaseDateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.termdates.Term.TermType
import scala.collection.JavaConverters._
import org.joda.time.{Interval, DateTime}
import uk.ac.warwick.tabula.AcademicYear

trait TermService {
	def getTermFromDate(date: BaseDateTime): Term
	def getPreviousTerm(term: Term): Term
	def getNextTerm(term: Term): Term
	def getAcademicWeek(date: BaseDateTime, weekNumber: Int): Interval
	def getAcademicWeeksForYear(date: BaseDateTime): Seq[(Integer, Interval)]
	def getAcademicWeeksBetween(start:DateTime, end:DateTime): Seq[(AcademicYear,Int,Interval)]
	def getTermFromDateIncludingVacations(date: BaseDateTime): Term
	def getTermsBetween(start: BaseDateTime, end: BaseDateTime): Seq[Term]
	def getAcademicWeekForAcademicYear(date: BaseDateTime, academicYear: AcademicYear): Int
}

/**
 * Wraps TermFactory and adds more features.
 */
@Service
class TermServiceImpl extends TermService {
	val termFactory = new TermFactoryImpl

	def getTermFromDate(date: BaseDateTime) = termFactory.getTermFromDate(date)

	def getPreviousTerm(term: Term) = termFactory.getPreviousTerm(term)

	def getNextTerm(term: Term) = termFactory.getNextTerm(term)

	def getAcademicWeek(date: BaseDateTime, weekNumber: Int) = termFactory.getAcademicWeek(date, weekNumber)

	def getAcademicWeeksForYear(date: BaseDateTime) = termFactory.getAcademicWeeksForYear(date).asScala map { pair => pair.getLeft -> pair.getRight }

	/**
	 * Return all the academic weeks for the specifed range, as a tuple of year, weeknumber, date interval
	 */
	def getAcademicWeeksBetween(start:DateTime, end:DateTime):Seq[(AcademicYear,Int,Interval)] = {
		val targetInterval = new Interval(start, end)

		val autumnTerms:Seq[Term]= termFactory.getTermDates.asScala
			.filter(t => t.getTermType == TermType.autumn)
			.filter(t=>t.getStartDate.isAfter(start.minusYears(1))) //go back a year to get the current year's autumn term
			.filter(t=> !(t.getStartDate.isAfter(end)))

		// since we only picked the autumn terms from the termfactory,
		// the endDate's year will be correct for the academicyear
		val weeksInRelevantYears = autumnTerms.flatMap(term=>{
			val weeks = termFactory.getAcademicWeeksForYear(term.getEndDate).asScala
			weeks.map(week=>(AcademicYear(term.getEndDate.getYear), week.getLeft.toInt, week.getRight))
		})

		def overlapsOrStartMatchesInstant(int:Interval)={
			int.overlaps(targetInterval) || ((targetInterval.getStart == targetInterval.getEnd) && (targetInterval.getStart == int.getStart))
		}

		weeksInRelevantYears
			.filter{case (year, weekNumber, weekInterval)=> overlapsOrStartMatchesInstant(weekInterval) }
		  .filterNot(_._2 == 53) // don't include week 53, it's just a confusing alias for week 1
	}

	def getTermFromDateIncludingVacations(date: BaseDateTime) = {
		val term = termFactory.getTermFromDate(date)
		if (date.isBefore(term.getStartDate)) Vacation(termFactory.getPreviousTerm(term), term)
		else term
	}

	def getTermsBetween(start: BaseDateTime, end: BaseDateTime): Seq[Term] = {
		val startTerm = getTermFromDateIncludingVacations(start)
		val endTerm = getTermFromDateIncludingVacations(end)

		if (startTerm == endTerm) Seq(startTerm)
		else startTerm +: getTermsBetween(startTerm.getEndDate.plusDays(1), end)
	}

	def getAcademicWeekForAcademicYear(date: BaseDateTime, academicYear: AcademicYear): Int = {
		val termContainingYearStart = getTermFromDateIncludingVacations(academicYear.dateInTermOne)
		def findNextAutumnTermForTerm(term: Term): Term = {
			term.getTermType match {
				case TermType.autumn => term
				case _ => findNextAutumnTermForTerm(getNextTerm(term))
			}
		}
		if (date.isBefore(termContainingYearStart.getStartDate))
			Term.WEEK_NUMBER_BEFORE_START
		else if (date.isAfter(findNextAutumnTermForTerm(getNextTerm(termContainingYearStart)).getStartDate))
			Term.WEEK_NUMBER_AFTER_END
		else
			termContainingYearStart.getAcademicWeekNumber(date)
	}

}


/** Special implementation of Term to encapsulate the idea of a Vacation.
	* Our default TermFactory doesn't care about Vacations, it returns the
	* next term if you give it a date before a vacation.
	*/
case class Vacation(before: Term, after: Term) extends Term {
	// Starts the day after the previous term and ends the day before the new term
	def getStartDate = before.getEndDate.plusDays(1)
	def getEndDate = after.getStartDate.minusDays(1)

	def getTermType = null
	def getTermTypeAsString = before.getTermType match {
		case Term.TermType.autumn => "Christmas vacation"
		case Term.TermType.spring => "Easter vacation"
		case Term.TermType.summer => "Summer vacation"
	}

	def getWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")
	def getCumulativeWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")
	def getAcademicWeekNumber(date: BaseDateTime) = after.getAcademicWeekNumber(date)
}

trait TermServiceComponent {
	def termService: TermService
}

trait AutowiringTermServiceComponent extends TermServiceComponent {
	var termService = Wire[TermService]
}