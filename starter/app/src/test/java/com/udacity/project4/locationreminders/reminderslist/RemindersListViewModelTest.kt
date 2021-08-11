package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(maxSdk = Build.VERSION_CODES.P)
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var reminderListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var fakeRepository: FakeDataSource


    @Before
    fun setupStatisticsViewModel() {
        // Initialise the repository with no tasks.
        fakeRepository = FakeDataSource()

        reminderListViewModel = RemindersListViewModel(getApplicationContext(),fakeRepository)
    }
    @Test
    fun noReminders() = runBlockingTest {

        fakeRepository.deleteAllReminders()
        reminderListViewModel.loadReminders()

        assertThat(reminderListViewModel.showNoData.value, CoreMatchers.`is`(true))

    }
    @Test
    fun hasReminder() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 59.24485888,
            longitude = 18.0873486)

        fakeRepository.saveReminder(reminder)
        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()

        assertThat(reminderListViewModel.showLoading.value, CoreMatchers.`is`(true))
    }
}