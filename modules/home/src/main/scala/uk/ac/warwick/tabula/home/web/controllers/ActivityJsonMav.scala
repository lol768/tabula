package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.tabula.web.views.{MarkdownRenderer, TextRenderer, JSONView}
import uk.ac.warwick.tabula.data.model.Activity
import uk.ac.warwick.tabula.web.Mav
import org.joda.time.format.ISODateTimeFormat

trait ActivityJsonMav {
	self: MarkdownRenderer =>

	val DateFormat = ISODateTimeFormat.dateTimeNoMillis()

	def toModel(activities: Seq[Activity[_]]) = Map("items" -> activities.map { item =>
	// TODO this should actually be HTML, at the moment it's plain text.
		val html = item.message

		Map(
			"published" -> DateFormat.print(item.date),
			"priority" -> item.priority,
			"title" -> item.title,
			"url" -> item.url,
			"content" -> html,
			"verb" -> item.verb
		)
	})

	def toMav(activities: Seq[Activity[_]]) = Mav(
		new JSONView(Map("items" -> activities.map { item =>
		// TODO this should actually be HTML, at the moment it's plain text.
			val html = item.message
			Map(
				"published" -> DateFormat.print(item.date),
				"priority" -> item.priority,
				"title" -> item.title,
				"url" -> item.url,
				"content" -> html,
				"verb" -> item.verb
			)
		}))
	)


}
