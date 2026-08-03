package com.stan0ne.fourthbutton

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.stan0ne.fourthbutton.actions.AssistiveAction
import com.stan0ne.fourthbutton.settings.AppPreferences

/**
 * Lists the configurable system actions with a checkbox so the user can toggle
 * which ones appear in the floating menu, and supports long-press drag to
 * reorder. Order and enable state persist straight into [AppPreferences]; the
 * menu reads the same source on next open.
 *
 * Actions the current device cannot perform (e.g. Flashlight with no camera
 * flash) stay togglable but show a note, so the menu and the settings screen
 * stay consistent.
 */
class ActionToggleAdapter(
    private val preferences: AppPreferences,
    items: List<AssistiveAction>,
    private val capability: (AssistiveAction) -> Boolean = { true },
) : RecyclerView.Adapter<ActionToggleAdapter.Holder>() {

    fun interface Listener {
        fun onChanged()
    }

    var listener: Listener? = null

    private val items: MutableList<AssistiveAction> = items.toMutableList()

    constructor(
        preferences: AppPreferences,
        context: Context,
        capability: (AssistiveAction) -> Boolean,
        onChanged: () -> Unit,
    ) : this(
        preferences,
        resolveOrderedActions(preferences),
        capability,
    ) {
        this.listener = Listener { onChanged() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action_toggle, parent, false)
        return Holder(view as android.widget.CheckBox)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val action = items[position]
        val enabled = preferences.isActionEnabled(action.id)
        val available = capability(action)
        val context = holder.itemView.context
        val label = context.getString(titleRes(action)) +
            if (available) "" else " (${context.getString(R.string.not_supported_on_device)})"
        holder.check.text = label
        holder.check.isChecked = enabled
        holder.check.setOnCheckedChangeListener { _, isChecked ->
            preferences.setActionEnabled(action.id, isChecked)
            listener?.onChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    fun currentOrder(): List<String> = items.map { it.id }

    fun move(from: Int, to: Int) {
        if (from == to) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
        listener?.onChanged()
    }

    private fun titleRes(action: AssistiveAction): Int = when (action) {
        AssistiveAction.SCREEN_LOCK -> R.string.screen_lock
        AssistiveAction.SCREENSHOT -> R.string.screenshot
        AssistiveAction.POWER_MENU -> R.string.power_menu
        AssistiveAction.FLASHLIGHT -> R.string.flashlight
        AssistiveAction.REBOOT -> R.string.reboot
    }

    class Holder(val check: android.widget.CheckBox) : RecyclerView.ViewHolder(check)

    private companion object {
        /** Stored order first, then any newly added actions in default order. */
        fun resolveOrderedActions(preferences: AppPreferences): List<AssistiveAction> {
            val stored = preferences.getActionOrder(AssistiveAction.DEFAULT_ORDER.map { it.id })
            val resolved = stored.mapNotNull { AssistiveAction.fromId(it) } +
                AssistiveAction.DEFAULT_ORDER.filter { !stored.contains(it.id) }
            return resolved.distinct().filter { it != AssistiveAction.REBOOT }
        }
    }
}
