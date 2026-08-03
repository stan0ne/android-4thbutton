package com.stan0ne.fourthbutton

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stan0ne.fourthbutton.accessibility.AccessibilityServiceState
import com.stan0ne.fourthbutton.accessibility.AssistiveAccessibilityService
import com.stan0ne.fourthbutton.actions.ActionRepository
import com.stan0ne.fourthbutton.databinding.ActivityMainBinding
import com.stan0ne.fourthbutton.settings.AppPreferences

/**
 * Companion activity UI, used only for first-run setup and configuration. It
 * shows the live accessibility-service state and lets the user choose which
 * actions appear in the floating menu, the screenshot delay and the floating
 * button appearance.
 *
 * Normal daily use is handled entirely by the floating overlay - no need to
 * open this screen again.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: AppPreferences
    private lateinit var actionRepository: ActionRepository
    private var actionAdapter: ActionToggleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = AppPreferences(this)
        actionRepository = ActionRepository(preferences, this)

        binding.enableButton.setOnClickListener { openAccessibilitySettings() }
        binding.delayImmediate.setOnClickListener { selectDelay(0) }
        binding.delay3.setOnClickListener { selectDelay(3000) }
        binding.delay5.setOnClickListener { selectDelay(5000) }
        binding.delay10.setOnClickListener { selectDelay(10000) }

        binding.sizeSmall.setOnClickListener { selectButtonSize(44) }
        binding.sizeMedium.setOnClickListener { selectButtonSize(56) }
        binding.sizeLarge.setOnClickListener { selectButtonSize(68) }
        binding.opacity40.setOnClickListener { selectButtonOpacity(0.4f) }
        binding.opacity60.setOnClickListener { selectButtonOpacity(0.6f) }
        binding.opacity80.setOnClickListener { selectButtonOpacity(0.8f) }
        binding.opacity100.setOnClickListener { selectButtonOpacity(1f) }

        actionAdapter = ActionToggleAdapter(
            preferences = preferences,
            context = this,
            capability = { actionRepository.isActionAvailable(it) },
            onChanged = { /* no extra work needed */ },
        )
        binding.actionList.layoutManager = LinearLayoutManager(this)
        binding.actionList.adapter = actionAdapter
        attachReorderSupport()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        highlightDelaySelection()
        highlightButtonSelection()
    }

    private fun refreshUi() {
        val enabled = AccessibilityServiceState.isEnabled(this)
        if (enabled) {
            binding.serviceStatus.text = getString(R.string.accessibility_enabled)
            binding.serviceHint.visibility = View.GONE
            binding.enableButton.visibility = View.GONE
            binding.floatingStatus.visibility = View.VISIBLE
            binding.floatingStatus.text = getString(R.string.floating_button_enabled)
        } else {
            binding.serviceStatus.text = getString(R.string.accessibility_disabled)
            binding.serviceHint.visibility = View.VISIBLE
            binding.enableButton.visibility = View.VISIBLE
            binding.floatingStatus.visibility = View.GONE
        }
        actionAdapter?.notifyDataSetChanged()
    }

    private fun selectDelay(ms: Int) {
        preferences.setScreenshotDelayMs(ms)
        highlightDelaySelection()
    }

    private fun highlightDelaySelection() {
        val current = preferences.getScreenshotDelayMs()
        binding.delayImmediate.isSelected = current == 0
        binding.delay3.isSelected = current == 3000
        binding.delay5.isSelected = current == 5000
        binding.delay10.isSelected = current == 10000
    }

    private fun selectButtonSize(sizeDp: Int) {
        preferences.setButtonSize(sizeDp)
        AssistiveAccessibilityService.applySettings()
        highlightButtonSelection()
    }

    private fun selectButtonOpacity(opacity: Float) {
        preferences.setButtonOpacity(opacity)
        AssistiveAccessibilityService.applySettings()
        highlightButtonSelection()
    }

    private fun highlightButtonSelection() {
        val size = preferences.getButtonSize()
        binding.sizeSmall.isSelected = size == 44
        binding.sizeMedium.isSelected = size == 56
        binding.sizeLarge.isSelected = size == 68

        val opacity = preferences.getButtonOpacity()
        binding.opacity40.isSelected = (opacity * 100).toInt() == 40
        binding.opacity60.isSelected = (opacity * 100).toInt() == 60
        binding.opacity80.isSelected = (opacity * 100).toInt() == 80
        binding.opacity100.isSelected = (opacity * 100).toInt() >= 100
    }

    private fun attachReorderSupport() {
        val adapter = actionAdapter ?: return
        val helper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ): Int = makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0,
            )

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                adapter.move(viewHolder.adapterPosition, target.adapterPosition)
                preferences.setActionOrder(adapter.currentOrder())
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = true
        })
        helper.attachToRecyclerView(binding.actionList)
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}