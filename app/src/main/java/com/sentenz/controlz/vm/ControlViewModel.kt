package com.sentenz.controlz.vm

import com.sentenz.controlz.base.BaseViewModel
import com.sentenz.controlz.R

/**
 * A VM for [com.sentenz.controlz.view.ControlActivity]
 *
 * MVVM usage from: https://gist.github.com/BapNesS/b9ec14b9f55131f2d172078f514d7e72
 */
class ControlViewModel : BaseViewModel() {
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
        setMessage(R.string.s_title_control)
//        navigateTo(R.id.controlActivity)
/*

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
}
