package com.stan0ne.fourthbutton.actions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.stan0ne.fourthbutton.util.LogUtil

/**
 * Controls the device torch via the public [CameraManager] API.
 *
 * It finds a rear camera unit with a flash, exposes the current state and
 * toggles it safely. Because the camera can be busy, every call is guarded with
 * try/catch so the app never crashes. Listener-based sync over [Listener] keeps
 * the floating menu indicator in step with our own toggled state.
 */
class FlashlightController(context: Context) {

    interface Listener {
        fun onStateChanged(enabled: Boolean)
    }

    private val cameraManager: CameraManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } else null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<Listener>()
    private var enabled = false

    fun isAvailable(): Boolean = findTorchCameraId() != null
    fun isOn(): Boolean = enabled

    fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.onStateChanged(enabled)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun toggle(): Boolean = setEnabled(!enabled)

    fun setEnabled(target: Boolean): Boolean {
        val id = findTorchCameraId() ?: run {
            LogUtil.w(LogUtil.TORCH, "no torch-capable camera found")
            return false
        }
        return try {
            val cm = cameraManager ?: return false
            cm.setTorchMode(id, target)
            enabled = target
            LogUtil.i(LogUtil.TORCH, "torch ${if (target) "on" else "off"} (camera=$id)")
            notifyListeners()
            true
        } catch (t: Throwable) {
            LogUtil.e(LogUtil.TORCH, "setTorchMode failed: ${t.message}")
            false
        }
    }

    private fun findTorchCameraId(): String? {
        val cm = cameraManager ?: return null
        return try {
            cm.cameraIdList.firstOrNull { id ->
                val cc = cm.getCameraCharacteristics(id)
                cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun notifyListeners() {
        mainHandler.post {
            listeners.toList().forEach { it.onStateChanged(enabled) }
        }
    }
}