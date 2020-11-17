package com.sentenz.controlz.base

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sentenz.controlz.utils.extension.toast

/**
 * Base for other activities
 *
 * From: https://gist.github.com/BapNesS/08429d955d1afa9205a9ce1ed2115052
 */
abstract class BaseActivity : AppCompatActivity() {

    abstract val baseViewModel: BaseViewModel?

    // Get a ViewModel that is a BaseViewModel
    protected inline fun <reified T : BaseViewModel> provideViewModel(): T = ViewModelProviders.of(this)[T::class.java]

    /**
     * When a message event is thrown, handle it and show it
     */
    protected open fun initObservers() {
        baseViewModel?.message?.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { message ->
                // Toast the [message]
                toast(message)
            }
        })
    }

}