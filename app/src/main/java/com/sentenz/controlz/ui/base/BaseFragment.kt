package com.sentenz.controlz.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sentenz.controlz.BR
import com.sentenz.controlz.utils.extension.navigateTo
import com.sentenz.controlz.utils.extension.snack
import kotlin.reflect.KClass
import kotlin.reflect.KDeclarationContainer

abstract class BaseFragment<Binding : ViewDataBinding, VM : BaseViewModel> : Fragment() {

    abstract val layoutId: Int
    abstract val viewModelClass: KClass<VM>

    open val variableId: Int = BR.viewModel

    protected lateinit var viewModel: VM
    protected lateinit var binding: Binding

    open fun onViewModelInitialised() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(viewModelClass.java)

        onViewModelInitialised()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.setVariable(variableId, viewModel)
//        binding.lifecycleOwner = this
//        binding.executePendingBindings()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.events.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let {

                when(it) {
                    is ShowSnackbar -> (activity?.currentFocus ?: binding.root).snack(it)
                    is NavigateTo -> navigateTo(it)
                }
            }
        })
    }
}
