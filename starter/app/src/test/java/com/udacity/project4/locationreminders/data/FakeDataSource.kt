package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var ReminderDTOs:MutableList<ReminderDTO>?= mutableListOf()) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source
    private var shouldReturnError = false

    fun setShouldReturnError(shouldReturn: Boolean) {
        this.shouldReturnError = shouldReturn
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        ReminderDTOs?.let { return Result.Success(ArrayList(it)) }
        if (shouldReturnError) {
            return Result.Error(
                    Exception("Test exception").toString()
            )
        }
        return Result.Error(
                Exception("Reminders not found").toString())
    }
    override suspend fun saveReminder(reminder: ReminderDTO) {
        ReminderDTOs?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = ReminderDTOs?.find { it.id == id }

        if (reminder != null) {
            return Result.Success(reminder)
        } else {
            if (shouldReturnError) {
                return Result.Error(
                        Exception("Test exception").toString()
                )
            }
            return Result.Error(Exception("Reminders not found").toString())
        }

    }

    override suspend fun deleteAllReminders() {
        ReminderDTOs?.clear()
    }
}