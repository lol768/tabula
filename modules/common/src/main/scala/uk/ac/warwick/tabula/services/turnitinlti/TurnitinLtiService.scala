package uk.ac.warwick.tabula.services.turnitinlti

import java.io.IOException

import dispatch.classic.Request.toRequestVerbs
import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.{HttpStatus, HttpRequest, HttpResponse}
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{FileAttachment, OriginalityReport, Assignment}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import com.google.api.client.auth.oauth.OAuthHmacSigner
import com.google.gdata.client.authn.oauth.{OAuthUtil, OAuthParameters}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.CurrentUser
import org.xml.sax.SAXParseException
import uk.ac.warwick.tabula.services.AutowiringOriginalityReportServiceComponent
import org.apache.commons.io.FilenameUtils._
import uk.ac.warwick.tabula.api.web.Routes
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import scala.util.{Failure, Success, Try}

object TurnitinLtiService {

	val AssignmentPrefix = "Assignment-"

	/**
	 * Quoted supported types are...
	 * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format".
	 */
	val validExtensions = Seq("doc", "docx", "pdf", "rtf", "txt", "wpd", "htm", "html", "ps", "odt")

	def validFileType(file: FileAttachment): Boolean =
		validExtensions contains getExtension(file.name).toLowerCase

	/**
	 * ID that we should store classes under. They are per-module so we base it on the module code.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same class.
	 */
	def classIdFor(assignment: Assignment, prefix: String) = ClassId(s"$prefix-${assignment.module.code}")

	/**
	 * ID that we should store assignments under. Our assignment ID is as good an identifier as any.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same assignment.
	 */
	def assignmentIdFor(assignment: Assignment) = AssignmentId(s"${AssignmentPrefix}${assignment.id}")

	def classNameFor(assignment: Assignment) = {
		val module = assignment.module
		ClassName(s"${module.code.toUpperCase}-${module.name}")
	}

	def assignmentNameFor(assignment: Assignment) = {
		AssignmentName(s"${assignment.name}(${assignment.academicYear.toString})")
	}
}

/**
 * Service for accessing the Turnitin LTI plagiarism API.
 */
@Service("turnitinLTIService")
class TurnitinLtiService extends Logging with DisposableBean with InitializingBean
	with AutowiringOriginalityReportServiceComponent {

	/** Turnitin LTI account id */
	@Value("${TurnitinLti.aid}") var turnitinAccountId: String = null
	/** Shared key as set up on the University of Warwick account's LTI settings */
	@Value("${TurnitinLti.key}") var sharedSecretKey: String = null

	@Value("${TurnitinLti.submitassignment.url}") var apiSubmitAssignment: String = _
	@Value("${TurnitinLti.submitpaper.url}") var apiSubmitPaperEndpoint: String = _
	@Value("${TurnitinLti.listendpoints.url}") var apiListEndpoints: String = _
	@Value("${TurnitinLti.submissiondetails.url}") var apiSubmissionDetails: String = _
	@Value("${TurnitinLti.reportlaunch.url}") var apiReportLaunch: String = _

	@Value("${turnitin.class.prefix}") var classPrefix: String =_

	@Value("${toplevel.url}") var topLevelUrl: String = _

	val userAgent = "Tabula, Coursework submission app, University of Warwick, coursework@warwick.ac.uk"

	val DateFormat = ISODateTimeFormat.dateTimeNoMillis()

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy() {
		http.shutdown()
	}

	override def afterPropertiesSet() {}

	def submitAssignment(assignment: Assignment, user: CurrentUser): TurnitinLtiResponse = {
		doRequest(
			apiSubmitAssignment,
			Map(
				"roles" -> "Instructor",
				"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
				"resource_link_title" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"resource_link_description" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
				"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
				"custom_duedate" -> DateFormat.print(new DateTime().plusYears(2)), // default is 7 days in the future, so make it far in future
				"ext_resource_tool_placement_url" -> s"$topLevelUrl${Routes.turnitin.submitAssignmentCallback(assignment)}"
			) ++ userParams(user.email, user.firstName, user.lastName)) {
			request =>
			// expect a 302
				request >:+ {
					(headers, request) =>
						val location = headers("location").headOption
						if (!location.isDefined) throw new IllegalStateException(s"Expected a redirect url")
							request >- {
							(html) => {
								// listen to callback for actual response
								TurnitinLtiResponse.redirect(location.get)
							}
						}
				}
		}
	}

	/**
	 * Expected response xml for a successful submission
			<response>
				<status>fullsuccess</status>
				<submission_data_extract>
						Some text
			</submission_data_extract>
				<lis_result_sourcedid>[Turnitin Submission ID]</lis_result_sourcedid>
				<message>Your file has been saved successfully.</message>
			</response>
	 */
	def submitPaper(
		assignment: Assignment,	paperUrl: String, userEmail: String, attachment: FileAttachment, userFirstName: String, userLastName: String
	 ): TurnitinLtiResponse = doRequest(

		s"$apiSubmitPaperEndpoint/${assignment.turnitinId}",
		Map(
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
			"custom_xmlresponse" -> "1",
			// or Instructor, but must supply an author user id, whatever the parameter for that is!!!
			"roles" -> "Learner",
			// I hoped this would be the callback Turnitin uses when a paper has been processed - apparently not
			// "ext_outcomes_tool_placement_url" ->  s"$topLevelUrl/api/tunitin-outcomes",
			"custom_submission_url" -> paperUrl,
			"custom_submission_title" -> attachment.id,
			"custom_submission_filename" -> attachment.name

		)
			++ userParams(userEmail, userFirstName, userLastName) ) {
		request =>
			request >:+ {
				(headers, request) =>
					request <>  {
						(node) => {
							val response = TurnitinLtiResponse.fromXml(node)
							if (response.success) {
								val originalityReport = originalityReportService.getOriginalityReportByFileId(attachment.id)
								if (originalityReport.isDefined) {
									originalityReport.get.turnitinId = response.turnitinSubmissionId()
									originalityReport.get.reportReceived = false
								} else {
									val report = new OriginalityReport
									report.turnitinId = response.turnitinSubmissionId()
									attachment.originalityReport = report
									originalityReportService.saveOriginalityReport(attachment)
								}
							}	else logger.warn("Failed to upload '" + attachment.name + "' - " + response.statusMessage.getOrElse(""))
							response
						}
					}
			}

	}

	def getSubmissionDetails(turnitinSubmissionId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiSubmissionDetails/$turnitinSubmissionId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def getOriginalityReportUrl(assignment: Assignment, attachment: FileAttachment, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiReportLaunch/${attachment.originalityReport.turnitinId}", Map(
			"roles" -> "Instructor",
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value
		) ++ userParams(user.email, user.firstName, user.lastName),
		expectedStatusCode = Some(HttpStatus.SC_MOVED_TEMPORARILY)) {
		request =>
			request >:+ {
				(headers, request) =>
					val location = headers("location").headOption
					// TODO we could parse the html instead of throwing an exception
					/** If document cannot be found, we expect the following html
					<div id="api_errorblock">
						<h2>Sorry, we could not process your request</h2>
						<p>The requested Object Result could not be found.</p>
					</div>
						**/
					if (!location.isDefined) throw new IllegalStateException(s"Expected a redirect url")
					request >- {
						(html) => {
							TurnitinLtiResponse.redirect(location.get)
						}
					}
			}
	}

	def listEndpoints(turnitinAssignmentId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiListEndpoints/$turnitinAssignmentId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def ltiConformanceTest(endpoint: String, secret: String, user: CurrentUser) = {
				doRequestForLtiTesting(
			endpoint,
				secret,
				Map(
					"custom_debug" -> "true",
					"resource_link_id" -> "1234567"
				)){
					request =>
						request >:+ {
							(headers, request) =>
								request >- {
									(html) => {
										TurnitinLtiResponse.fromHtml(html.contains("message request is valid"), html)
									}
								}
						}
				}
	}

	def userParams(email: String, firstName: String, lastName: String): Map[String, String] = {
		Map(
			"user_id" -> email,
			"lis_person_contact_email_primary" -> email,
			"lis_person_contact_name_given" -> firstName,
			"lis_person_contact_name_family" -> lastName)
	}

	def commonParameters = Map(
		"lti_message_type" -> "basic-lti-launch-request",
		"lti_version" -> "LTI-1p0",
		"User-Agent" -> userAgent
	)

	def doRequestForLtiTesting(
		endpoint: String, secret: String, params: Map[String, String]
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint, Some(secret))

		val req = (url(endpoint) <:< Map()).POST << signedParams

		try {
			http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: SAXParseException =>
					logger.error("Unexpected response from provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def doRequest(
	endpoint: String, params: Map[String, String], expectedStatusCode: Option[Int] = None
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint, None)

		val req = (url(endpoint) <:< Map()).POST << signedParams

		logger.debug("doRequest: " + signedParams)
		try {
			if (expectedStatusCode.isDefined){
				Try(http.when(_==expectedStatusCode.get)(transform(req))) match {
					case Success(response) => response
					case Failure(StatusCode(code, contents)) =>
						logger.warn(s"Not expected http status code")
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"), responseCode = Some(code))
					case _ =>
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"))
				}
			} else http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting Turnitin", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: java.lang.Exception =>
					logger.error("Some other exception", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def getSignedParams(params: Map[String, String], endpoint: String, optionalSecret: Option[String]): Map[String, String] = {
		val hmacSigner = new OAuthHmacSigner()
		hmacSigner.clientSharedSecret = optionalSecret.getOrElse(sharedSecretKey)

		val oauthparams = new OAuthParameters()
		oauthparams.setOAuthConsumerKey(turnitinAccountId)
		oauthparams.setOAuthNonce(OAuthUtil.getNonce)
		oauthparams.setOAuthTimestamp(OAuthUtil.getTimestamp)
		oauthparams.setOAuthSignatureMethod("HMAC-SHA1")
		oauthparams.setOAuthCallback("about:blank")

		val allParams = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala

		val signatureBaseString = OAuthUtil.getSignatureBaseString(endpoint, "POST", allParams.asJava)
		val signature = hmacSigner.computeSignature(signatureBaseString)

		oauthparams.addCustomBaseParameter("oauth_signature", signature)
		val allParamsIncludingSignature = commonParameters ++ params ++ oauthparams.getBaseParameters.asScala ++ oauthparams.getExtraParameters.asScala
		allParamsIncludingSignature
	}
}

trait TurnitinLtiServiceComponent {
	def turnitinLtiService: TurnitinLtiService
}

trait AutowiringTurnitinLtiServiceComponent extends TurnitinLtiServiceComponent {
	var turnitinLtiService = Wire[TurnitinLtiService]
}

case class ClassName(value: String)

case class ClassId(value: String)

case class AssignmentName(value: String)

case class AssignmentId(value: String)