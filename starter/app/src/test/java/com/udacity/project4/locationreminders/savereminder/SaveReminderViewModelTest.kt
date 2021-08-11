package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import org.hamcrest.MatcherAssert.assertThat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(maxSdk = Build.VERSION_CODES.P)
class SaveReminderViewModelTest {


    //TODO: provide testing to the SaveReminderView and its live data objects
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeRepository: FakeDataSource


    @Before
    fun setupStatisticsViewModel() {
        stopKoin()
        // Initialise the repository with no tasks.
        fakeRepository = FakeDataSource()

        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),fakeRepository)
        runBlocking{ fakeRepository.deleteAllReminders()}
    }

    @Test
    fun saveReminder() {
        val reminder = ReminderDataItem(
            title = "title",
            description = "description",
            location = "loc",
            latitude = 47.5456551,
            longitude = 122.0101731)
        saveReminderViewModel.saveReminder(reminder)
        assertThat(saveReminderViewModel.showToast.value, Is.`is`("Reminder Saved !"))
    }
    @Test
    fun noTitle() = runBlockingTest {
        val reminder = ReminderDataItem(
            title = "",
            description = "hey",
            location = "loc",
            latitude = 47.5456551,
            longitude = 122.0101731)
        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(saveReminderViewModel.showSnackBarInt.value, CoreMatchers.notNullValue())

    }
    @Test
    fun noLocation() {

        val reminder = ReminderDataItem(
            title = "hey",
            description = "hey",
            location = "",
            latitude = 47.5456551,
            longitude = 122.0101731)

        saveReminderViewModel.validateAndSaveReminder(reminder)
        assertThat(saveReminderViewModel.showSnackBarInt.value,
            CoreMatchers.notNullValue()
        )

    }

}