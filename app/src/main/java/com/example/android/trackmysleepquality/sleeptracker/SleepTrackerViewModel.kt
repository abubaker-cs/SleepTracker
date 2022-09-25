/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    // private var viewModelJob = Job()

    // It will cancel all active coroutines
    // override fun onCleared() {
    //    super.onCleared()
    //    viewModelJob.cancel()
    // }

    // Scope for the Coroutines to run in.
    // private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // *************************************** ^^^^ NOT NEEDED

    // Use the "tonight" liveData variable and use a coroutine to initialize it from the database
    // "tonight" will hold the current night, and make it MutableLiveData
    private var tonight = MutableLiveData<SleepNight?>()

    // Get records of all nights using the .getAllNights() function defined in SleepDatabaseDao
    private val nights = database.getAllNights()

    // Local CRUD functions: insert(), update() and clear()
    init {
        initializeTonight()
    }

    private fun initializeTonight() {

        // Coroutine - Get tonight's info from the database
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }

    }

    // We are using suspend because we want to call this function from inside the coroutine and not the block.
    private suspend fun getTonightFromDatabase(): SleepNight? {

        // This returns the latest night saved in the database
        var night = database.getTonight()

        // If the start and end times are the same, then we know that we are continuing with an existing night
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }

        return night

    }

    // Click Handlers for: Start(), Stop() and Clear() buttons (using coroutines)

    // Click Handler - Button: START
    suspend fun onStartTracking() {
        val newNight = SleepNight()
        insert(newNight)
        tonight.value = getTonightFromDatabase()
    }

    // Click Handler - Button: STOP
    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
        }
    }

    // Click Handler - Button: CLEAR
    fun onClear() {
        viewModelScope.launch {
            clear()
            tonight.value = null
        }
    }

    // ---------------------------------------------------------

    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    private suspend fun update(night: SleepNight) {
        database.update(night)
    }

    private suspend fun clear() {
        database.clear()
    }

    // Transform nights into a nightsString using formatNights()
    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }


}

