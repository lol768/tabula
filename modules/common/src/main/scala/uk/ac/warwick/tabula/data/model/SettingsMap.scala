package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

trait HasSettings {

	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	protected var settings: Map[String, Any] = Map()

	protected def settingsIterator = settings.iterator

	protected def getSetting(key: String) = settings.get(key)

	protected def getStringSetting(key: String) = settings.get(key) match {
		case Some(value: String) => Some(value)
		case _ => None
	}
	protected def getIntSetting(key: String) = settings.get(key) match {
		case Some(value: Int) => Some(value)
		case _ => None
	}
	protected def getBooleanSetting(key: String) = settings.get(key) match {
		case Some(value: Boolean) => Some(value)
		case _ => None
	}
	protected def getStringSeqSetting(key: String) = settings.get(key) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}
	protected def getStringMapSetting(key: String) = {		
		settings.get(key) match {
			case Some(value: Map[_, _]) => Some(value.asInstanceOf[Map[String, String]])
			case Some(value: collection.mutable.Map[_, _]) => Some(value.toMap.asInstanceOf[Map[String, String]])
			case _ => None
		}
	}

	protected def getStringSetting(key: String, default: => String): String = getStringSetting(key) getOrElse(default)
	protected def getIntSetting(key: String, default: => Int): Int = getIntSetting(key) getOrElse(default)
	protected def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key) getOrElse(default)
	protected def getStringSeqSetting(key: String, default: => Seq[String]): Seq[String] = getStringSeqSetting(key) getOrElse(default)
	protected def getStringMapSetting(key: String, default: => Map[String, String]): Map[String, String] = getStringMapSetting(key) getOrElse(default)

	protected def settingsSeq = settings.toSeq

	protected def ensureSettings {
		if (settings == null) settings = Map()
	}
}

/**
 * Danger!
 *
 * Exposing the ++= methods publicly means that other classes can directly manipulate the keys of this map.
 * If your class expects keys to have certain specified values (e.g. enums) then mix in HasSettings and
 * expose type-safe getters and setters instead of mixing in SettingsMap directly
 */

trait SettingsMap extends HasSettings {

	protected def -=(key: String): this.type = {
		settings -= key
		this
	}
	
	protected def +=(kv: (String, Any)): this.type = {
		settings += kv
		this
	}

	protected def ++=(sets: Pair[String, Any]*): this.type = {
		settings ++= sets
		this
	}

	def ++=(other: SettingsMap): this.type = {
		this.settings ++= other.settings
		this
	}
}