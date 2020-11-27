/*
 * Copyright 2020 Sentenz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sentenz.controlz.base

import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.sentenz.controlz.R
import com.sentenz.controlz.utils.livedata.Event
import kotlin.reflect.KClass

/**
 * Base for other ViewModel
 *
 * From: https://gist.github.com/BapNesS/4e23073e553e2016e029df50d9c43829#file-baseviewmodel-kt-L7
 */
abstract class BaseViewModel: ViewModel() {

    val events = MutableLiveData<Event<CommonAction>>()

    private fun postAction(commonAction: CommonAction) = events.postValue(Event(commonAction))

    fun navigateTo(
            @IdRes navigationTargetId: Int,
            clearBackStack: Boolean = false,
            args: Bundle? = null,
            navOptions: NavOptions? = null,
            extras: Navigator.Extras? = null
    ) = postAction(NavigateTo(
            navigationTargetId = navigationTargetId,
            clearBackStack = clearBackStack,
            args = args,
            navOptions = navOptions,
            extras = extras
    ))

    fun showErrorSnackBar(message: String) = showSnackbar(ShowSnackbar(
            message = message,
            colorRes = R.color.colorPrimary
    ))

    private fun showSnackbar(snackbarEvent: ShowSnackbar) = postAction(snackbarEvent)

    /** Mutable/LiveData of String resource reference Event */
    private val _message = MutableLiveData<Event<Int>>()
    val message : LiveData<Event<Int>>
        get() = _message

    /** Post in background thread */
    fun postMessage(@StringRes message: Int) {
        _message.postValue(Event(message))
    }

    /** Post in main thread */
    fun setMessage(@StringRes message: Int) {
        _message.value = Event(message)
    }

    init {
        Log.i("BaseViewModel", "BaseViewModel created!")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("BaseViewModel", "BaseViewModel destroyed!")
    }
}

/*
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.sentenz.controlz.R
import com.sentenz.controlz.utils.livedata.Event

abstract class BaseViewModel : ViewModel() {

    val events = MutableLiveData<Event<CommonAction>>()

    private fun postAction(commonAction: CommonAction) = events.postValue(Event(commonAction))

    fun navigateTo(
            @IdRes navigationTargetId: Int,
            clearBackStack: Boolean = false,
            args: Bundle? = null,
            navOptions: NavOptions? = null,
            extras: Navigator.Extras? = null
    ) = postAction(NavigateTo(
            navigationTargetId = navigationTargetId,
            clearBackStack = clearBackStack,
            args = args,
            navOptions = navOptions,
            extras = extras
    ))

    fun showErrorSnackBar(message: String) = showSnackbar(ShowSnackbar(
            message = message,
            colorRes = R.color.colorPrimary
    ))

    private fun showSnackbar(snackbarEvent: ShowSnackbar) = postAction(snackbarEvent)
}
*/

