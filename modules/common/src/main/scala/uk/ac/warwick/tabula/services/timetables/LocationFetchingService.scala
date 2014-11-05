package uk.ac.warwick.tabula.services.timetables

import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.springframework.beans.factory.DisposableBean
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation, Location}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.util.cache.{Caches, CacheEntryFactory}
import uk.ac.warwick.util.web.Uri
import scala.util.{Failure, Success, Try}
import scala.util.parsing.json.JSON

import scala.collection.JavaConverters._

/**
 * A service that takes a location as a String, and tries to turn it into a
 * MapLocation, if at all possible, else it will fall back to a NamedLocation.
 */
trait LocationFetchingService {
	def locationFor(name: String): Location
}

class CachedLocationFetchingService(delegate: LocationFetchingService) extends LocationFetchingService with AutowiringCacheStrategyComponent {

	val CacheExpiryTime = 60 * 60 * 48 // 48 hours in seconds

	val cacheEntryFactory = new CacheEntryFactory[String, Location] {
		def create(name: String): Location = delegate.locationFor(name)

		def create(names: JList[String]): JMap[String, Location] = {
			JMap(names.asScala.map(name => (name, create(name))): _*)
		}

		def isSupportsMultiLookups = true
		def shouldBeCached(location: Location) = true
	}

	lazy val cache =
		Caches.newCache("LocationCache", cacheEntryFactory, CacheExpiryTime, cacheStrategy)

	def locationFor(name: String) = cache.get(name)

}

trait WAI2GoConfiguration {
	def baseUri: Uri
	def cached: Boolean
	def defaultProperties: Map[String, String]
	def queryParameter: String
}

trait WAI2GoConfigurationComponent {
	def wai2GoConfiguration: WAI2GoConfiguration
}

trait AutowiringWAI2GoConfigurationComponent extends WAI2GoConfigurationComponent {
	val wai2GoConfiguration = new AutowiringWAI2GoConfiguration
	class AutowiringWAI2GoConfiguration extends WAI2GoConfiguration {
		val cached = true
		val baseUri = Uri.parse("https://campus.warwick.ac.uk/Services/api.php")
		val defaultProperties = Map(
			"config" -> "warwick",
			"act" -> "ac",
			"mlim" -> "2",
			"limit" -> "2",
			"type" -> "lb",
			"key" -> "$2a$11$ra7c/DofvF6yZuQhl.SBEuLrA8k2fvyt.WlJ8bbI.Asd1OyT.N2JS"
		)
		val queryParameter = "q"
	}
}

trait LocationFetchingServiceComponent {
	def locationFetchingService: LocationFetchingService
}

trait WAI2GoHttpLocationFetchingServiceComponent extends LocationFetchingServiceComponent {
	self: WAI2GoConfigurationComponent =>

	lazy val locationFetchingService = WAI2GoHttpLocationFetchingService(wai2GoConfiguration)
}

private class WAI2GoHttpLocationFetchingService(config: WAI2GoConfiguration) extends LocationFetchingService with Logging with DisposableBean {

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy {
		http.shutdown()
	}

	// a dispatch response handler which reads JSON from the response and parses it into a list of TimetableEvents
	// the timetable response doesn't include its year, so we pass that in separately.
	def handler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req >- { (rawJSON) =>
			JSON.parseFull(rawJSON) match {
				case Some(locations: Seq[Map[String, Any]] @unchecked) => locations.flatMap(WAI2GoLocation.fromProperties)
				case _ => Nil
			}
		}
	}

	def locationFor(name: String) = {
		val req = url(config.baseUri.toString) <<? (config.defaultProperties ++ Map(config.queryParameter -> name))

		// Execute the request
		logger.info(s"Requesting location info for $name")
		Try(http.when(_==200)(req >:+ handler)) match {
			case Success(locations)
				if locations.size == 1 =>
					MapLocation(locations.head.name, locations.head.locationId)

			case Success(locations) =>
				logger.info(s"Multiple locations returned for $name, returning NamedLocation")
				NamedLocation(name)

			case Failure(ex) =>
				logger.warn(s"Error requesting location information for $name, returning NamedLocation", ex)
				NamedLocation(name)
		}
	}
}

object WAI2GoHttpLocationFetchingService {
	def apply(config: WAI2GoConfiguration) = {
		val service = new WAI2GoHttpLocationFetchingService(config)

		if (config.cached) {
			new CachedLocationFetchingService(service)
		} else {
			service
		}
	}
}

case class WAI2GoLocation(name: String, subLocation: String, locationId: String)
object WAI2GoLocation {
	def fromProperties(properties: Map[String, Any]): Option[WAI2GoLocation] = {
		val name = properties.get("name").collect { case s: String => s }
		val locationId = properties.get("lid").collect { case s: String => s }
		val subLocation = properties.get("sub").collect { case s: String => s }

		if (name.nonEmpty && locationId.nonEmpty) {
			Some(WAI2GoLocation(name.get, subLocation.getOrElse(""), locationId.get))
		} else {
			None
		}
	}
}