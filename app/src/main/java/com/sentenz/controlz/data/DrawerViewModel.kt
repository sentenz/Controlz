package com.sentenz.controlz.data

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.sentenz.controlz.ControlActivity
import com.sentenz.controlz.PaternosterActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.sentenz.controlz.base.BaseViewModel
import com.sentenz.controlz.R

/**
 * A VM for [com.sentenz.controlz.ui.DrawerActivity].
 */
class DrawerViewModel : BaseViewModel() {
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

    /* CardView onClick callback */
    fun onControlClicked() {
/*
        navigateTo(
                navigationTargetId = R.id.action_drawerActivity_to_controlActivity,
        )

        val intent = Intent(this, ControlActivity::class.java)
        startActivity(intent)

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

    fun onOdometerClicked() {
    }

    fun onPaternosterClicked() {
//        val intent = Intent(this, PaternosterActivity::class.java)
//        startActivity(intent)
    }
}
