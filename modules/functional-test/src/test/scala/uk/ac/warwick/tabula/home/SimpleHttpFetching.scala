package uk.ac.warwick.tabula.home

import dispatch.classic._
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import dispatch.classic.thread.ThreadSafeHttpClient

trait SimpleHttpFetching {
	lazy val http: Http = new Http with thread.Safety with NoLogging {

		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}
}