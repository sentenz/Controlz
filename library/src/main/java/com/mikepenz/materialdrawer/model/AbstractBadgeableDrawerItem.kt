package com.mikepenz.materialdrawer.model

import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.mikepenz.materialdrawer.R
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.interfaces.ColorfulBadgeable
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem

/**
 * An abstract [IDrawerItem] implementation describing an element which supports badges
 */
abstract class AbstractBadgeableDrawerItem<Item : AbstractBadgeableDrawerItem<Item>> : BaseDescribeableDrawerItem<Item, AbstractBadgeableDrawerItem.ViewHolder>(), ColorfulBadgeable {
    override var badge: StringHolder? = null
    override var badgeStyle: BadgeStyle? = BadgeStyle()

    override/*"PRIMARY_ITEM"*/ val type: Int
        get() = R.id.material_drawer_item_primary

    override val layoutRes: Int
        @LayoutRes
        get() = R.layout.material_drawer_item_primary

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        val ctx = holder.itemView.context
        //bind the basic view parts
        bindViewHelper(holder)

        //set the text for the badge or hide
        val badgeVisible = StringHolder.applyToOrHide(badge, holder.badge)
        //style the badge if it is visible
        if (badgeVisible) {
            badgeStyle?.style(holder.badge, getColor(ctx))
            holder.badge.visibility = View.VISIBLE
        } else {
            holder.badge.visibility = View.GONE
        }

        //define the typeface for our textViews
        if (typeface != null) {
            holder.badge.typeface = typeface
        }

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, holder.itemView)
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    open class ViewHolder(view: View) : BaseViewHolder(view) {
        internal val badge: TextView = view.findViewById(R.id.material_drawer_badge)
    }
}
