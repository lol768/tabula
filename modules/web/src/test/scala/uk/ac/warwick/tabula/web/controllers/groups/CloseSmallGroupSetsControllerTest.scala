package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.commands.groups.admin.OpenSmallGroupSet
import uk.ac.warwick.tabula.web.controllers.groups.admin.OpenAllSmallGroupSetsController
import uk.ac.warwick.tabula.{SmallGroupFixture, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class CloseSmallGroupSetsControllerTest extends TestBase with Mockito {

	@Test
	def createsViewModelAbleToBuildCommand() {
		val controller = new OpenAllSmallGroupSetsController()
		controller.newViewModelOpen(new Department, SmallGroupSetSelfSignUpState.Closed) should be(anInstanceOf[controller.GroupsetListViewModel])
		controller.newViewModelOpen(new Department, SmallGroupSetSelfSignUpState.Closed).createCommand(new User, Nil) should be(anInstanceOf[OpenSmallGroupSet])
	}


	@Test
	def exposesOnlySelfSignupGroupSetsToForm() {
		new SmallGroupFixture {

			groupSet1.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
			groupSet2.allocationMethod = SmallGroupAllocationMethod.Manual
			department.modules = Seq(groupSet1.module, groupSet2.module).asJava
			department.code = "XYZ"

			val controller = new OpenAllSmallGroupSetsController()
			val mav = controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Closed), department)

			mav.map("groupSets") should be(Seq(groupSet1))
		}
	}

	@Test
	def exposesDepartmentToForm() {
		new SmallGroupFixture {
			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"

			val mav = controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Closed), department)

			mav.map("department") should be(department)

		}
	}

	@Test
	def exposesFlashStatusToForm() {
		new SmallGroupFixture {

			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"

			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Closed), department).map("showFlash") should be(JBoolean(Some(false)))
			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Closed), department, true).map("showFlash") should be(JBoolean(Some(true)))
		}
	}

	@Test
	def usesCorrectViewNameForForm() {
		new SmallGroupFixture {

			val controller = new OpenAllSmallGroupSetsController()
			department.code = "XYZ"

			controller.form(controller.newViewModelOpen(department, SmallGroupSetSelfSignUpState.Closed), department).viewName should be("groups/admin/groups/bulk-open")
		}
	}

	@Test
	def viewModelPassesApplyOntoCommand() {
		val controller = new OpenAllSmallGroupSetsController()
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]

		val model = new controller.GroupsetListViewModel((u, s) => mockCommand, SmallGroupSetSelfSignUpState.Closed)

		model.applyCommand(new User)

		verify(mockCommand, times(1)).apply()
	}


	@Test
	def submitSendsRedirectBackToOpenGroupsPage() {
		new SmallGroupFixture {
			department.code = "XYZ"
			val user = new CurrentUser(new User, new User)
			withCurrentUser(user) {
				val controller = new OpenAllSmallGroupSetsController()
				val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
				val viewModel = new controller.GroupsetListViewModel((u, s) => mockCommand, SmallGroupSetSelfSignUpState.Closed)

				val mav = controller.submit(viewModel, department)

				mav.viewName should be("redirect:/groups/admin/department/XYZ/groups/selfsignup/close")
				mav.map("batchOpenSuccess") should be (JBoolean(Some(true)))
			}
		}
	}
}