package uk.ac.warwick.courses.commands
import collection.mutable

trait Describable {
	def describe(d:Description)
	val eventName:String
}

/**
 * Stateful instance of an action in the application.
 * It could be anything that we might want to keep track of,
 * especially if we might want to audit log it. Anything that
 * adds or changes any data is a candidate. Read-only queries,
 * not so much.
 */
trait Command[R] extends Describable {
	def apply(): R
	lazy val eventName = getClass.getSimpleName.replaceAll("Command$","")
}

/**
 * Trait that can only be applied to a Command, which marks it
 * out of the audit log (the default is to audit).
 * 
 * The self type declaration is ostensibly to allow you to access
 * methods on Command via "self" (as you would "this"), but here
 * it's just to enforce the class type.
 */
trait Unaudited { self: Command[_] =>
	
}

/**
 * Object for a Command to describe what it's just done.
 */
abstract class Description {
	protected var map = mutable.Map[String,Any]()
	def properties(props: Pair[String,Any]*) = {
		map ++= props
		this
	}
	def properties(otherMap: Map[String,Any]) = {
		map ++= otherMap
		this
	}
	def property(prop: Pair[String,Any]) = {
		map += prop
		this
	}
	
	// delegate equality to the underlying map
	override def hashCode = map.hashCode
	override def equals(that:Any) = that match {
		case d:Description => map.equals(d.map)
		case _ => false
	}
}

class DescriptionImpl extends Description {
	def allProperties = map
}