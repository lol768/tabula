package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.{TestBase, Mockito}
import org.junit.Before
import java.util.Properties
import uk.ac.warwick.tabula.helpers.ArrayList
import freemarker.core.Environment
import freemarker.template.Template
import java.io.StringReader
import java.io.StringWriter
import freemarker.template.SimpleHash
import freemarker.template.TemplateModel
import freemarker.template.TemplateDirectiveBody

class UrlMethodModelTest extends TestBase with Mockito {
	
	val model: UrlMethodModel = new UrlMethodModel
	
	@Before def setup() {
		model.context = "/courses"
		model.toplevelUrl = "https://courses.warwick.ac.uk"
		model.staticHashes = new Properties
	}
	
	@Test def fn() {
		model.exec(ArrayList("/module/yes", "/")).toString should be ("/module/yes")
		model.exec(ArrayList("/module/yes")).toString should be ("/courses/module/yes")
		model.exec(ArrayList("/module/yes", "/profiles")).toString should be ("/profiles/module/yes")
	}
	
	@Test def encoding() {
		val input = "/download/greek \u03a7\u03a8\u03a9.doc"
		val expected = "/courses/download/greek%20%CE%A7%CE%A8%CE%A9.doc"
		model.exec(ArrayList(input)).toString should be (expected)
	}
	
	@Test def tagPageAndContext() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash
		hash.put("page", "/module/yes")
		hash.put("context", "/")
		
		val writer = new StringWriter
		
		val env = new Environment(new Template("temp", new StringReader("")), hash, writer)
		val body = mock[TemplateDirectiveBody]
		
		val params = new java.util.HashMap[String, TemplateModel]
		params.put("page", hash.get("page"))
		params.put("context", hash.get("context"))
		
		model.execute(env, params, null, body)
				
		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/module/yes")
	}
	
	@Test def tagPageNoContext() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash
		hash.put("page", "/module/yes")
		
		val writer = new StringWriter
		
		val env = new Environment(new Template("temp", new StringReader("")), hash, writer)
		val body = mock[TemplateDirectiveBody]
		
		val params = new java.util.HashMap[String, TemplateModel]
		params.put("page", hash.get("page"))
		
		model.execute(env, params, null, body)
				
		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/courses/module/yes")
	}
	
	@Test def tagResource() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash
		hash.put("resource", "/css/main.css")
		
		model.staticHashes.setProperty("css/main.css", "1234567890")
		
		val writer = new StringWriter
		
		val env = new Environment(new Template("temp", new StringReader("")), hash, writer)
		val body = mock[TemplateDirectiveBody]
		
		val params = new java.util.HashMap[String, TemplateModel]
		params.put("resource", hash.get("resource"))
		
		model.execute(env, params, null, body)
				
		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/css/main.css.1234567890")
	}

}