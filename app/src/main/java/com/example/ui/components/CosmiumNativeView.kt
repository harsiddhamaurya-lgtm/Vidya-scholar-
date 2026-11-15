package com.example.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

object CosmiumNativeBridge {
    @Volatile
    private var isLibraryLoaded = false
    @Volatile
    private var loadAttempted = false

    fun ensureLoaded(): Boolean {
        if (!loadAttempted) {
            synchronized(this) {
                if (!loadAttempted) {
                    loadAttempted = true
                    isLibraryLoaded = try {
                        System.loadLibrary("cosmium_native")
                        true
                    } catch (_: UnsatisfiedLinkError) {
                        false
                    }
                }
            }
        }
        return isLibraryLoaded
    }

    @JvmStatic external fun nativeCreate(): Long
    @JvmStatic external fun nativeInit(handle: Long, width: Int, height: Int)
    @JvmStatic external fun nativeResize(handle: Long, width: Int, height: Int)
    @JvmStatic external fun nativeRender(
        handle: Long,
        dt: Float,
        state: String,
        interactionIntensity: Float,
        moodTurbulence: Float,
        colorHex: Int
    )
    @JvmStatic external fun nativeDestroy(handle: Long)
}

class CosmiumGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: CosmiumRendererWrapper

    @Volatile var state: String = "idle"
    @Volatile var interactionIntensity: Float = 0.0f
    @Volatile var moodTurbulence: Float = 0.0f
    @Volatile var colorHex: Int = 0xFF9F1C
    @Volatile private var isReleased = false

    init {
        CosmiumNativeBridge.ensureLoaded()
        setEGLContextClientVersion(3)
        setEGLConfigChooser(SafeTranslucentConfigChooser())
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        preserveEGLContextOnPause = true
        renderer = CosmiumRendererWrapper(this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class SafeTranslucentConfigChooser : EGLConfigChooser {
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

    private class CosmiumRendererWrapper(
        private val view: CosmiumGLSurfaceView
    ) : Renderer {
        private var lastTimeNano = System.nanoTime()
        var nativeHandle: Long = 0L
            private set

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            if (nativeHandle != 0L) {
                CosmiumNativeBridge.nativeDestroy(nativeHandle)
                nativeHandle = 0L
            }
            if (view.isReleased) return
            nativeHandle = CosmiumNativeBridge.nativeCreate()
            CosmiumNativeBridge.nativeInit(
                nativeHandle,
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1)
            )
            lastTimeNano = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            if (nativeHandle != 0L && !view.isReleased) {
                CosmiumNativeBridge.nativeResize(nativeHandle, width.coerceAtLeast(1), height.coerceAtLeast(1))
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            val handle = nativeHandle
            if (handle == 0L || view.isReleased) return

            val now = System.nanoTime()
            val dt = ((now - lastTimeNano) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
            lastTimeNano = now

            CosmiumNativeBridge.nativeRender(
                handle,
                dt,
                view.state,
                view.interactionIntensity,
                view.moodTurbulence,
                view.colorHex
            )
        }

        fun releaseOnGlThread() {
            val handle = nativeHandle
            nativeHandle = 0L
            if (handle != 0L) {
                CosmiumNativeBridge.nativeDestroy(handle)
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
fun CosmiumNativeView(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    state: String = "idle",
    interactionIntensity: Float = 0.0f,
    moodTurbulence: Float = 0.0f,
    colorHex: Int = 0xFFE066
) {
    if (!isActive) return

    val nativeAvailable = remember { CosmiumNativeBridge.ensureLoaded() }
    if (!nativeAvailable) {
        val fallbackMood = when (state) {
            "speaking" -> CosmiumMood.SPEAKING
            "thinking", "processing" -> CosmiumMood.THINKING
            "curious", "listening" -> CosmiumMood.CURIOUS
            "celebrate", "correct" -> CosmiumMood.CELEBRATE
            "alert" -> CosmiumMood.ALERT
            else -> CosmiumMood.IDLE
        }
        CosmiumOrb(
            size = 64.dp,
            mood = fallbackMood,
            interactionIntensity = interactionIntensity,
            modifier = modifier
        )
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceViewHolder = remember { arrayOfNulls<CosmiumGLSurfaceView>(1) }

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
            CosmiumGLSurfaceView(context).apply {
                this.state = state
                this.interactionIntensity = interactionIntensity
                this.moodTurbulence = moodTurbulence
                this.colorHex = colorHex
                surfaceViewHolder[0] = this
            }
        },
        update = { view ->
            surfaceViewHolder[0] = view
            view.state = state
            view.interactionIntensity = interactionIntensity
            view.moodTurbulence = moodTurbulence
            view.colorHex = colorHex
        }
    )
}
