package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var ReminderDTOs:MutableList<ReminderDTO>?= mutableListOf()) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        ReminderDTOs?.let { return Result.Success(ArrayList(it)) }
        return Result.Error(
            "Reminders not found",404
        )
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        ReminderDTOs?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = ReminderDTOs?.find { it.id == id }

        if (reminder != null) {
            return Result.Success(reminder)
        } else {
            return Result.Error("Reminders not found",404)
        }

    }

    override suspend fun deleteAllReminders() {
        ReminderDTOs?.clear()
    }
}