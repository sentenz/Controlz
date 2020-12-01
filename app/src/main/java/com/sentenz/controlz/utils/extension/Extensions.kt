package com.sentenz.controlz.utils.extension

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.sentenz.controlz.R
import com.sentenz.controlz.ui.base.NavigateTo
import com.sentenz.controlz.ui.base.ShowSnackbar

fun View.snack(showSnackbarEvent: ShowSnackbar): Snackbar = with(showSnackbarEvent) {
    val whiteSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorWhite_1000))
    val snackbarText = SpannableStringBuilder(message)
    snackbarText.setSpan(whiteSpan, 0, snackbarText.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

    val snackbar = Snackbar.make(this@snack, snackbarText, length)
    snackbar.view.setBackgroundColor(ContextCompat.getColor(context, colorRes))

    snackbar.show()

    return snackbar
}

fun Fragment.navigateTo(navigateToEvent: NavigateTo) {
    view?.findNavController()?.navigateTo(navigateToEvent)
//            ?: Toast.makeText(this, "Could not find navigation controller", Toast.LENGTH_SHORT).show()
}

fun NavController.navigateTo(navigateToEvent: NavigateTo) {
    var navOptions = navigateToEvent.navOptions

    if (navigateToEvent.clearBackStack) {
        navOptions = navOptions.setClearBackStack(graph.id)
    }

    navigate(navigateToEvent.navigationTargetId, navigateToEvent.args, navOptions)
}

private fun NavOptions?.setClearBackStack(graphId: Int): NavOptions = if (this == null) {
    NavOptions.Builder()
            .setPopUpTo(graphId, true)
            .build()
} else {
    NavOptions.Builder()
            .setEnterAnim(enterAnim)
            .setPopUpTo(graphId, true)
            .setExitAnim(exitAnim)
            .setPopEnterAnim(popEnterAnim)
            .setPopExitAnim(popExitAnim)
            .setLaunchSingleTop(shouldLaunchSingleTop())
            .build()

}
