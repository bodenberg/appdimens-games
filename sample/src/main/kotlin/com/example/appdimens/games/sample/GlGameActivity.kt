package com.example.appdimens.games.sample

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.SurfaceHolder
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * [EN] OpenGL ES game demo: continuous render loop using the Kotlin kernels.
 * `onSurfaceChanged` publishes the new snapshot — every subsequent frame auto-adjusts.
 *
 * [PT] Demo de jogo em OpenGL ES: loop de render contínuo com os kernels Kotlin.
 * `onSurfaceChanged` publica o novo snapshot — todo frame seguinte se ajusta sozinho.
 */
class GlGameActivity : Activity() {

    private lateinit var glView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(GameRenderer(this@GlGameActivity))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        setContentView(glView)
    }

    override fun onResume() { super.onResume(); glView.onResume() }
    override fun onPause() { super.onPause(); glView.onPause() }
}

internal class GameRenderer(private val activity: Activity) : GLSurfaceView.Renderer {

    private val vertexShader = """
        attribute vec4 aPos;
        uniform vec4 uColor;
        varying vec4 vColor;
        void main() { gl_Position = aPos; vColor = uColor; }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        varying vec4 vColor;
        void main() { gl_FragColor = vColor; }
    """.trimIndent()

    private var program = 0
    private var posLoc = 0
    private var colorLoc = 0
    private var aspect = 1f
    private var surfaceW = 1f
    private var surfaceH = 1f
    private val startNs = System.nanoTime()

    // Quad template (unit), uploaded once; scaled per draw via buffer rewrite (no allocs after init).
    private lateinit var quadBuffer: FloatBuffer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GameScreen.updateFromContext(activity, fullscreen = true)
        program = buildProgram()
        posLoc = android.opengl.GLES20.glGetAttribLocation(program, "aPos")
        colorLoc = android.opengl.GLES20.glGetUniformLocation(program, "uColor")
        quadBuffer = ByteBuffer.allocateDirect(6 * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        android.opengl.GLES20.glClearColor(0.039f, 0.047f, 0.070f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        android.opengl.GLES20.glViewport(0, 0, width, height)
        aspect = if (height > 0) width.toFloat() / height else 1f
        surfaceW = width.coerceAtLeast(1).toFloat()
        surfaceH = height.coerceAtLeast(1).toFloat()
        // AUTO-ADJUST HOOK: publish the resized window to the whole library.
        GameScreen.updateFromContext(activity, fullscreen = true)
    }

    override fun onDrawFrame(gl: GL10?) {
        val m = GameScreen.metrics()
        val t = (System.nanoTime() - startNs) / 1_000_000_000f

        // Strategy sizes in px (single-multiply fast lanes).
        val playerPx = GameMath.toPx(GameMath.calculateAutoDp(48f, m), m) / 2f
        val enemyPx = GameMath.toPx(GameMath.calculateAutoDp(32f, m), m) / 2f

        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
        android.opengl.GLES20.glUseProgram(program)

        // Convert px → clip space ([-1..1]) using the live surface size.
        fun pxToClip(px: Float, axisPx: Float) = px * 2f / axisPx

        // Player at center
        drawQuad(0f, 0f, pxToClip(playerPx, surfaceW), pxToClip(playerPx, surfaceH),
            floatArrayOf(0f, 0.898f, 1f, 1f))

        // Orbiting enemies
        repeat(8) { i ->
            val a = t * 0.9f + i * (2f * Math.PI.toFloat() / 8)
            val x = cos(a.toDouble()).toFloat() * 0.7f
            val y = sin(a.toDouble()).toFloat() * 0.7f
            val hue = i / 8f
            drawQuad(x, y, pxToClip(enemyPx, surfaceW), pxToClip(enemyPx, surfaceH),
                floatArrayOf(hue, 1f - hue, 0.5f, 1f))
        }
    }

    private fun drawQuad(cx: Float, cy: Float, halfW: Float, halfH: Float, rgba: FloatArray) {
        val b = quadBuffer
        b.rewind()
        b.put(cx - halfW); b.put(cy + halfH); b.put(0f)
        b.put(cx - halfW); b.put(cy - halfH); b.put(0f)
        b.put(cx + halfW); b.put(cy + halfH); b.put(0f)
        b.put(cx + halfW); b.put(cy + halfH); b.put(0f)
        b.put(cx - halfW); b.put(cy - halfH); b.put(0f)
        b.put(cx + halfW); b.put(cy - halfH); b.put(0f)
        b.rewind()

        android.opengl.GLES20.glVertexAttribPointer(posLoc, 3, android.opengl.GLES20.GL_FLOAT, false, 0, b)
        android.opengl.GLES20.glEnableVertexAttribArray(posLoc)
        android.opengl.GLES20.glUniform4fv(colorLoc, 1, rgba, 0)
        android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLES, 0, 6)
    }

    private fun buildProgram(): Int {
        fun compile(type: Int, src: String): Int {
            val s = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(s, src)
            android.opengl.GLES20.glCompileShader(s)
            return s
        }
        val p = android.opengl.GLES20.glCreateProgram()
        android.opengl.GLES20.glAttachShader(p, compile(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShader))
        android.opengl.GLES20.glAttachShader(p, compile(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        android.opengl.GLES20.glLinkProgram(p)
        return p
    }

}
