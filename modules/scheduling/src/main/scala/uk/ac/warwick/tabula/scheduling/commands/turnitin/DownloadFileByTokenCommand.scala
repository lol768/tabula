package uk.ac.warwick.tabula.scheduling.commands.turnitin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions._
import org.springframework.validation.Errors
import org.joda.time.DateTime

object DownloadFileByTokenCommand {
	def apply(submission: Submission, fileAttachment: FileAttachment, token: FileAttachmentToken ) = 
		new DownloadFileByTokenCommandInternal(submission, fileAttachment, token)
		with ComposableCommand[RenderableFile]
		with DownloadFileByTokenCommandState
		with DownloadFileByTokenValidation
		with DownloadFileByTokenDescription
		with ReadOnly
		with PubliclyVisiblePermissions
}

class DownloadFileByTokenCommandInternal (
		val submission: Submission,
		val fileAttachment: FileAttachment,
		val token: FileAttachmentToken ) extends CommandInternal[RenderableFile]{

	self: DownloadFileByTokenCommandState =>

	override def applyInternal() = {

		val attachment = Option(new RenderableAttachment(fileAttachment))

		fileFound = attachment.isDefined
		token.dateUsed = new DateTime()
		attachment.get

	}

}

trait DownloadFileByTokenCommandState {

	def submission: Submission
	def fileAttachment: FileAttachment
	def token: FileAttachmentToken

	var fileFound: Boolean = _
}

trait DownloadFileByTokenValidation extends SelfValidating {

	self: DownloadFileByTokenCommandState =>

	override def validate(errors:Errors) {
		if (Option(token.dateUsed).isDefined){
			errors.reject("filedownload.token.used")
		}	else if(token.expires.isBeforeNow){
			errors.reject("filedownload.token.expired")
		} else if (!token.fileAttachmentId.equals(fileAttachment.id)) {
			errors.reject("filedownload.token.invalid")
		}
	}
}

trait DownloadFileByTokenDescription extends Describable[RenderableFile] {

	self: DownloadFileByTokenCommandState =>

	override def describe(d: Description) = {
		d.submission(submission)
		d.fileAttachments(Seq(fileAttachment))
	}

}