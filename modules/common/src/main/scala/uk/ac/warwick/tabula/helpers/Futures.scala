package uk.ac.warwick.tabula.helpers

import org.hibernate.{FlushMode, SessionFactory}
import org.springframework.orm.hibernate4.{SessionFactoryUtils, SessionHolder}
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.ac.warwick.spring.Wire

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait Futures {

	def flatten[A, M[X] <: TraversableOnce[X]](in: Seq[Future[M[A]]])(implicit executor: ExecutionContext): Future[Seq[A]] = {
		val p = scala.concurrent.Promise[Seq[A]]

		// If any of the Futures fail, fire the first failure up to the Promise
		in.foreach { _.onFailure { case t => p.tryFailure(t) } }

		// Get the sequential result of the futures and flatten them
		Future.sequence(in).foreach { results => p.trySuccess(results.flatten) }

		p.future
	}

}

object Futures extends Futures {

	/**
		* One big problem with threads is that Hibernate binds the current session to a thread local, and then other
		* things depend on it. What's a safe way to do that? Well there isn't one, really. We can open a new read-only
		* session and bind that, though, which is better than always having a null session.
		*/
	implicit lazy val executionContext = new ExecutionContext {
		private lazy val sessionFactory: Option[SessionFactory] = Wire.option[SessionFactory]

		override def execute(runnable: java.lang.Runnable): Unit = sessionFactory match {
			case None => ExecutionContext.Implicits.global.execute(runnable)
			case Some(sf) => ExecutionContext.Implicits.global.execute(Runnable {
				val session = sf.openSession()
				session.setDefaultReadOnly(true)
				session.setFlushMode(FlushMode.MANUAL)

				val sessionHolder = new SessionHolder(session)

				try {
					TransactionSynchronizationManager.bindResource(sf, sessionHolder)
					runnable.run()
				} finally {
					TransactionSynchronizationManager.unbindResourceIfPossible(sf)
					SessionFactoryUtils.closeSession(session)
				}
			})
		}
		override def reportFailure(cause: Throwable): Unit = ExecutionContext.Implicits.global.reportFailure(cause)
	}

}