package com.sentenz.controlz.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.sentenz.controlz.utils.extension.toast
import kotlin.reflect.KClass

/**
 * Base for other activities
 *
 * From: https://gist.github.com/BapNesS/08429d955d1afa9205a9ce1ed2115052
 */
abstract class BaseActivity<Binding : ViewDataBinding, VM : BaseViewModel> : AppCompatActivity() {

    abstract val layoutId: Int
    abstract val viewModelClass: KClass<VM>

    protected lateinit var viewModel: VM
    protected lateinit var binding: Binding

    open fun onViewModelInitialised() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(viewModelClass.java)

        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.setVariable(1, viewModel)
        binding.lifecycleOwner = this
        binding.executePendingBindings()

        onViewModelInitialised()
        onObservers()
    }

    /** When a message event is thrown, handle it and show it */
    protected open fun onObservers() {
        viewModel?.message?.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { message ->
                /** Toast the [message] */
                toast(message)
            }
        })
    }
}