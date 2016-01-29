package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver._

import scala.collection.JavaConversions.asScalaBuffer


class DownloadMeetingRecordFilesCommand (val meetingRecord: AbstractMeetingRecord) extends Command[Option[RenderableFile]] with ReadOnly {

	PermissionCheck(Permissions.Profiles.MeetingRecord.ReadDetails(meetingRecord.relationship.relationshipType), meetingRecord)

	var filename: String = _

	var zipService = Wire.auto[ZipService]

	private var fileFound: Boolean = _

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def applyInternal() = {
		val result: Option[RenderableFile] =
			filename match {
				case filename: String if filename.hasText => {
					meetingRecord.attachments.find(_.name == filename).map(new RenderableAttachment(_))
				}
				case _ => Some(zipped(meetingRecord))
			}

		fileFound = result.isDefined
		result
	}

	private def zipped(meetingRecord: AbstractMeetingRecord) = zipService.getSomeMeetingRecordAttachmentsZip(meetingRecord)

	override def describe(d: Description) = {
		d.meeting(meetingRecord)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}
}