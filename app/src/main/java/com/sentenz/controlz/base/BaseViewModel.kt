package com.sentenz.controlz.base

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sentenz.controlz.utils.livedata.Event

/**
 * Base for other ViewModel
 *
 * From: https://gist.github.com/BapNesS/4e23073e553e2016e029df50d9c43829#file-baseviewmodel-kt-L7
 */
abstract class BaseViewModel: ViewModel() {

    /* Mutable/LiveData of String resource reference Event */
    private val events = MutableLiveData<Event<Int>>()
    val message : LiveData<Event<Int>>
        get() = events

    /* Post in background thread */
    fun postMessage(@StringRes message: Int) {
        events.postValue(Event(message))
    }

    /* Post in main thread */
    fun setMessage(@StringRes message: Int) {
        events.value = Event(message)
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

