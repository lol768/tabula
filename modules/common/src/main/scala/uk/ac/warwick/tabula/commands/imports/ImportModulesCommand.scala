package uk.ac.warwick.tabula.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire

class ImportModulesCommand extends Command[Unit] with Logging with Daoisms {

	var moduleImporter = Wire.auto[ModuleImporter]
	var moduleService = Wire.auto[ModuleAndDepartmentService]

	def applyInternal() {
		transactional() {
			importDepartments
			importModules
		}
	}

	def describe(d: Description) {

	}

	def importModules {
		logger.info("Importing modules")
		for (dept <- moduleService.allDepartments) {
			importModules(moduleImporter.getModules(dept.code), dept)
		}
	}

	def importModules(modules: Seq[ModuleInfo], dept: Department) {
		for (mod <- modules) {
			moduleService.getModuleByCode(mod.code) match {
				case None => {
					debug("Mod code %s not found in database, so inserting", mod.code)
					session.saveOrUpdate(newModuleFrom(mod, dept))
				}
				case Some(module) => {
					// HFC-354 Update module name if it changes.
					if (mod.name != module.name) {
						logger.info("Updating name of %s to %s".format(mod.code, mod.name))
						module.name = mod.name
						session.saveOrUpdate(module)
					}
				}
			}
		}
	}

	def importDepartments {
		logger.info("Importing departments")
		for (dept <- moduleImporter.getDepartments) {
			moduleService.getDepartmentByCode(dept.code) match {
				case None => session.save(newDepartmentFrom(dept))
				case Some(dept) => {debug("Skipping %s as it is already in the database", dept.code) }
			}
		}
	}

	private def newModuleFrom(m: ModuleInfo, dept: Department): Module = {
		val module = new Module
		module.code = m.code
		module.name = m.name
		//      module.members.baseWebgroup = m.group
		module.department = dept
		module
	}

	private def newDepartmentFrom(d: DepartmentInfo): Department = {
		val department = new Department
		department.code = d.code
		department.name = d.name
		department
	}

}