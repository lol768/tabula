package uk.ac.warwick.courses

import java.util.Properties
import scala.collection.JavaConversions._
import scala.reflect.BeanInfo
import org.springframework.beans.BeanWrapperImpl
import org.springframework.core.env.PropertySource
import scala.reflect.BeanProperty
import scala.reflect.BooleanBeanProperty

@BeanInfo
class Features(properties:Properties) {
	
	//// Features /////
	
	@BooleanBeanProperty var emailStudents:Boolean =_
	
	//// END of features ///
	
	val featuresPrefix = "features."
	
	val bean = new BeanWrapperImpl(this)
	private def featureKeys = properties.keysIterator.filter( _ startsWith featuresPrefix )
	def capitalise(string:String) = string.head.toUpperCase + string.tail
	def removePrefix(string:String) = string.substring(featuresPrefix.length)
	def camelise(string:String) = removePrefix(string).split("\\.").toList match {
		case Nil => ""
		case head :: tail => head + tail.map(capitalise).mkString("")
	}
	
	for (key <- featureKeys) bean.setPropertyValue(camelise(key), properties.getProperty(key))
}
