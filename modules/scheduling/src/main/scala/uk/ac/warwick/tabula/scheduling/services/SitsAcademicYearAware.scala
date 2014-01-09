package uk.ac.warwick.tabula.scheduling.services

import org.springframework.jdbc.`object`.MappingSqlQuery
import java.sql.ResultSet
import javax.sql.DataSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConversions._
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.joda.time.DateTime

trait SitsAcademicYearAware {
	var sitsAcademicYearService = Wire[SitsAcademicYearService]

	def getCurrentSitsAcademicYearString: String = sitsAcademicYearService.getCurrentSitsAcademicYearString

	def getCurrentSitsAcademicYear: AcademicYear = {
		AcademicYear.parse(getCurrentSitsAcademicYearString)
	}

}

trait SitsAcademicYearService {
	def getCurrentSitsAcademicYearString: String
}

@Profile(Array("dev", "test", "production"))
@Service
class SitsAcademicYearServiceImpl extends SitsAcademicYearService {
	var sits = Wire[DataSource]("sitsDataSource")

	val GetCurrentAcademicYear = """
		select GET_AYR() ayr from dual
		"""

	def getCurrentSitsAcademicYearString: String = {
		new GetCurrentAcademicYearQuery(sits).execute().head
	}

	def getCurrentSitsAcademicYear: AcademicYear = {
		AcademicYear.parse(getCurrentSitsAcademicYearString)
	}

	class GetCurrentAcademicYearQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetCurrentAcademicYear) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int) = rs.getString("ayr")
	}
} 

@Profile(Array("sandbox")) 
@Service
class SandboxSitsAcademicYearService extends SitsAcademicYearService {
	
	def getCurrentSitsAcademicYearString: String = 
		AcademicYear.guessByDate(DateTime.now).toString()
}