package com.example.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicLong
import javax.microedition.khronos.egl.EGLConfig
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
        setEGLConfigChooser(SafeTranslucentEglConfigChooser())
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        preserveEGLContextOnPause = true
        renderer = DiceRendererWrapper(this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class DiceRendererWrapper(
        private val view: PasaDiceGLSurfaceView
    ) : Renderer {
        private var lastTimeNano = System.nanoTime()
        private val nativeHandle = AtomicLong(0L)

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            val prev = nativeHandle.getAndSet(0L)
            if (prev != 0L) {
                PasaDiceNativeBridge.nativeDestroy(prev)
            }
            if (view.isReleased) return
            val created = PasaDiceNativeBridge.nativeCreate()
            if (nativeHandle.compareAndSet(0L, created)) {
                PasaDiceNativeBridge.nativeInit(
                    created,
                    view.width.coerceAtLeast(1),
                    view.height.coerceAtLeast(1)
                )
            } else {
                PasaDiceNativeBridge.nativeDestroy(created)
            }
            lastTimeNano = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            val handle = nativeHandle.get()
            if (handle != 0L && !view.isReleased) {
                PasaDiceNativeBridge.nativeResize(handle, width.coerceAtLeast(1), height.coerceAtLeast(1))
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            val handle = nativeHandle.get()
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

        fun releaseNativeHandle() {
            val handle = nativeHandle.getAndSet(0L)
            if (handle != 0L) {
                PasaDiceNativeBridge.nativeDestroy(handle)
            }
        }
    }

    fun releaseNativeResources() {
        if (isReleased) return
        isReleased = true
        queueEvent {
            renderer.releaseNativeHandle()
        }
    }

    override fun onDetachedFromWindow() {
        releaseNativeResources()
        super.onDetachedFromWindow()
        renderer.releaseNativeHandle()
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

    val nativeAvailable = remember { CosmiumNativeBridge.ensureLoaded() }
    if (!nativeAvailable) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(accentColorHex or (0xFF shl 24)).copy(alpha = if (enabled) 0.92f else 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRolling) "..." else diceValue.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

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
