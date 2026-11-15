package com.example.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

object PasaDiceNativeBridge {
    @JvmStatic external fun nativeCreate(): Long
    @JvmStatic external fun nativeInit(handle: Long, width: Int, height: Int)
    @JvmStatic external fun nativeResize(handle: Long, width: Int, height: Int)
    @JvmStatic external fun nativeRender(
        handle: Long,
        dt: Float,
        diceValue: Int,
        isRolling: Boolean,
        accentColorHex: Int,
        enabled: Boolean
    )
    @JvmStatic external fun nativeDestroy(handle: Long)
}

class PasaDiceGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: DiceRendererWrapper

    @Volatile var diceValue: Int = 1
    @Volatile var isRolling: Boolean = false
    @Volatile var accentColorHex: Int = 0xFF9F1C
    @Volatile var diceEnabled: Boolean = true
    @Volatile private var isReleased = false

    init {
        CosmiumNativeBridge.ensureLoaded()
        setEGLContextClientVersion(3)
        setEGLConfigChooser(SafeTranslucentDiceConfigChooser())
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        preserveEGLContextOnPause = true
        renderer = DiceRendererWrapper(this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class SafeTranslucentDiceConfigChooser : EGLConfigChooser {
        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val eglOpenGlEs3Bit = 0x0040
            val eglOpenGlEs2Bit = 0x0004
            val candidates = listOf(
                intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_SAMPLE_BUFFERS, 1,
                    EGL10.EGL_SAMPLES, 4,
                    EGL10.EGL_RENDERABLE_TYPE, eglOpenGlEs3Bit,
                    EGL10.EGL_NONE
                ),
                intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_RENDERABLE_TYPE, eglOpenGlEs3Bit,
                    EGL10.EGL_NONE
                ),
                intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_RENDERABLE_TYPE, eglOpenGlEs2Bit,
                    EGL10.EGL_NONE
                ),
                intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_NONE
                ),
                intArrayOf(EGL10.EGL_NONE)
            )
            val numConfig = IntArray(1)
            for (spec in candidates) {
                if (egl.eglChooseConfig(display, spec, null, 0, numConfig) && numConfig[0] > 0) {
                    val configs = arrayOfNulls<EGLConfig>(numConfig[0])
                    if (egl.eglChooseConfig(display, spec, configs, configs.size, numConfig)) {
                        val best = configs.filterNotNull().firstOrNull()
                        if (best != null) return best
                    }
                }
            }
            throw IllegalArgumentException("No suitable EGLConfig available")
        }
    }

    private class DiceRendererWrapper(
        private val view: PasaDiceGLSurfaceView
    ) : Renderer {
        private var lastTimeNano = System.nanoTime()
        var nativeHandle: Long = 0L
            private set

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            if (nativeHandle != 0L) {
                PasaDiceNativeBridge.nativeDestroy(nativeHandle)
                nativeHandle = 0L
            }
            if (view.isReleased) return
            nativeHandle = PasaDiceNativeBridge.nativeCreate()
            PasaDiceNativeBridge.nativeInit(
                nativeHandle,
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1)
            )
            lastTimeNano = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            if (nativeHandle != 0L && !view.isReleased) {
                PasaDiceNativeBridge.nativeResize(nativeHandle, width.coerceAtLeast(1), height.coerceAtLeast(1))
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            val handle = nativeHandle
            if (handle == 0L || view.isReleased) return

            val now = System.nanoTime()
            val dt = ((now - lastTimeNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
            lastTimeNano = now

            PasaDiceNativeBridge.nativeRender(
                handle,
                dt,
                view.diceValue,
                view.isRolling,
                view.accentColorHex,
                view.diceEnabled
            )
        }

        fun releaseOnGlThread() {
            val handle = nativeHandle
            nativeHandle = 0L
            if (handle != 0L) {
                PasaDiceNativeBridge.nativeDestroy(handle)
            }
        }
    }

    fun releaseNativeResources() {
        if (isReleased) return
        isReleased = true
        queueEvent {
            renderer.releaseOnGlThread()
        }
    }

    override fun onDetachedFromWindow() {
        releaseNativeResources()
        super.onDetachedFromWindow()
    }
}

@Composable
fun PasaDiceNativeView(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    diceValue: Int,
    isRolling: Boolean,
    accentColorHex: Int,
    enabled: Boolean
) {
    if (!isActive) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceViewHolder = remember { arrayOfNulls<PasaDiceGLSurfaceView>(1) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surfaceViewHolder[0]?.onResume()
                Lifecycle.Event.ON_PAUSE -> surfaceViewHolder[0]?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceViewHolder[0]?.releaseNativeResources()
            surfaceViewHolder[0] = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PasaDiceGLSurfaceView(context).apply {
                this.diceValue = diceValue
                this.isRolling = isRolling
                this.accentColorHex = accentColorHex
                this.diceEnabled = enabled
                surfaceViewHolder[0] = this
            }
        },
        update = { view ->
            surfaceViewHolder[0] = view
            view.diceValue = diceValue
            view.isRolling = isRolling
            view.accentColorHex = accentColorHex
            view.diceEnabled = enabled
        }
    )
}
