package com.sentenz.controlz.ui.component.drawer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.sentenz.controlz.R
import com.sentenz.controlz.ui.base.BaseViewModel
import com.sentenz.controlz.ui.component.control.ControlActivity
import com.sentenz.controlz.ui.component.control.ControlFragment
import com.sentenz.controlz.ui.component.paternoster.PaternosterActivity


class DrawerViewModel : BaseViewModel() {

    /**
     * Callbacks
     */

    fun onControlClicked(view: View) {
        val context: Context = view.context
        val intent = Intent(context, ControlActivity::class.java)
        context.startActivity(intent)
/*
        MaterialDialog(this).show {
            title(text = "Test")
            message(text = "Do you want proceed? " + view.id.toString())
            positiveButton(text = "Agree") { dialog ->
            }
            negativeButton(text = "Disagree") { dialog ->
                // Do nothing
            }
        }
*/
    }

    fun onPaternosterClicked(view: View) {
        val context: Context = view.context
        val intent = Intent(context, PaternosterActivity::class.java)
        context.startActivity(intent)
    }

    fun onOdometerClicked(view: View) {
//        setMessage(R.string.list_title_odometer)
        if (view.context is AppCompatActivity) {
            val fragment = ControlFragment()
            (view.context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
//                    .add(R.id.frameLayout, fragment).commit()
                    .replace(R.id.frameLayout, fragment).commit()
//                .replace(R.id.fragment_container, fragment).addToBackStack("main").commit()
        }

//        val transaction: FragmentTransaction = (view.context as AppCompatActivity).supportFragmentManager.beginTransaction()
//        transaction.add(R.id.frameLayout, fragment)
//        transaction.commit()

    }

    /*
    private val _name = MutableLiveData("Ada")
    private val _lastName = MutableLiveData("Lovelace")
    private val _likes =  MutableLiveData(0)

    val name: LiveData<String> = _name
    val lastName: LiveData<String> = _lastName
    val likes: LiveData<Int> = _likes

    // popularity is exposed as LiveData using a Transformation instead of a @Bindable property.
    val popularity: LiveData<Popularity> = Transformations.map(_likes) {
        when {
            it > 9 -> Popularity.STAR
            it > 4 -> Popularity.POPULAR
            else -> Popularity.NORMAL
        }
    }

    fun onLike() {
        _likes.value = (_likes.value ?: 0) + 1
    }
*/
}
