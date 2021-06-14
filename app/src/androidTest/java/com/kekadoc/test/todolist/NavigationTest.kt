package com.kekadoc.test.todolist

import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.kekadoc.test.todolist.auth.AuthService
import com.kekadoc.test.todolist.auth.Credential
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun logInUi() {
        lateinit var authService: AuthService
        lateinit var navController: NavController
        activityRule.scenario.onActivity {
            authService = AuthService.getInstance(it)
            navController = it.findNavController(R.id.nav_host_fragment_activity_main)
            if (authService.isAuth()) authService.setCredential(null)
        }
        onView(ViewMatchers.withId(R.id.button_signIn)).perform(ViewActions.click())

        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.destination_login)

        val newCredential = Credential("Oleg", "1234")

        onView(ViewMatchers.withId(R.id.editText_name)).perform(ViewActions.replaceText(newCredential.login))
        onView(ViewMatchers.withId(R.id.editText_password)).perform(ViewActions.replaceText(newCredential.password))

        onView(ViewMatchers.withId(R.id.button_signIn)).perform(ViewActions.click())

        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.destination_all_tasks)

    }
    @Test
    fun logOutUi() {
        lateinit var authService: AuthService
        lateinit var navController: NavController
        activityRule.scenario.onActivity {
            authService = AuthService.getInstance(it)
            navController = it.findNavController(R.id.nav_host_fragment_activity_main)
            if (!authService.isAuth()) {
                val newCredential = Credential("Oleg", "1234")
                authService.setCredential(newCredential)
            }
        }
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(ViewMatchers.withText(R.string.fragment_all_tasks_toolbar_menu_item_logout)).perform(ViewActions.click())
        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.destination_login)

        Truth.assertThat(authService.isAuth()).isFalse()

    }
    @Test
    fun newTaskUi() {
        lateinit var authService: AuthService
        lateinit var navController: NavController
        activityRule.scenario.onActivity {
            authService = AuthService.getInstance(it)
            navController = it.findNavController(R.id.nav_host_fragment_activity_main)
            if (!authService.isAuth()) {
                val newCredential = Credential("Oleg", "1234")
                authService.setCredential(newCredential)
            }
        }
        onView(ViewMatchers.withId(R.id.button_add)).perform(ViewActions.click())
        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.destination_task_detailed)
    }
    @Test
    fun taskDetailedOptionsUi() {
        lateinit var authService: AuthService
        lateinit var navController: NavController
        activityRule.scenario.onActivity {
            authService = AuthService.getInstance(it)
            navController = it.findNavController(R.id.nav_host_fragment_activity_main)
            if (!authService.isAuth()) {
                val newCredential = Credential("Oleg", "1234")
                authService.setCredential(newCredential)
            }
        }

        onView(ViewMatchers.withId(R.id.button_add)).perform(ViewActions.click())
        onView(ViewMatchers.withContentDescription(R.string.fragment_task_detailed_option_more_title)).perform(
            ViewActions.click())

        Truth.assertThat(navController.currentDestination?.id).isEqualTo(R.id.destination_task_options)

    }


}