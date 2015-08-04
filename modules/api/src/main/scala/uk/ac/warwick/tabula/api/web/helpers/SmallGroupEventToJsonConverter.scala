package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation}
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, SmallGroupEvent}
import uk.ac.warwick.tabula.helpers.StringUtils._

trait SmallGroupEventToJsonConverter {
	def jsonSmallGroupEventObject(event: SmallGroupEvent): Map[String, Any] = {
		Map(
			"id" -> event.id,
			"title" -> event.title,
			"weeks" -> event.weekRanges.map { case WeekRange(min, max) =>
				Map(
					"minWeek" -> min,
					"maxWeek" -> max
				)
			},
			"day" -> event.day.name,
			"startTime" -> event.startTime.toString("HH:mm"),
			"endTime" -> event.startTime.toString("HH:mm"),
			"location" -> Option(event.location).map {
				case NamedLocation(name) => Map("name" -> name)
				case MapLocation(name, locationId) => Map("name" -> name, "locationId" -> locationId)
			}.orNull,
			"tutors" -> event.tutors.users.map { user =>
				Seq(
					user.getUserId.maybeText.map { "userId" -> _ },
					user.getWarwickId.maybeText.map { "universityId" -> _ }
				).flatten.toMap
			}
		)
	}
}