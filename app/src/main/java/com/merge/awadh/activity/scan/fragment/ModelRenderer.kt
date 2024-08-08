package com.merge.awadh.activity.scan.fragment

import android.content.res.AssetManager
import android.view.Choreographer
import android.view.SurfaceView
import android.graphics.Color
import android.graphics.PixelFormat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import android.graphics.drawable.GradientDrawable
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelRenderer {
    private lateinit var surfaceView: SurfaceView
    private lateinit var lifecycle: Lifecycle

    private lateinit var choreographer: Choreographer
    private lateinit var uiHelper: UiHelper

    private lateinit var modelViewer: ModelViewer
    private var filamentAsset: FilamentAsset? = null
    private var animator: Animator? = null
    private var animationStartTime: Long = 0

    private val assets: AssetManager
        get() = surfaceView.context.assets

    private val frameScheduler = FrameCallback()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            choreographer.postFrameCallback(frameScheduler)
        }

        override fun onPause(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            choreographer.removeFrameCallback(frameScheduler)
            lifecycle.removeObserver(this)
            cleanup()
            animator = null
        }
        private fun cleanup() {
            filamentAsset?.let { asset ->
                // Remove entities from the scene
                modelViewer.scene.removeEntities(asset.entities)

                // Destroy each entity associated with the asset
                asset.entities.forEach { entity ->
                    modelViewer.engine.destroyEntity(entity)
                }

                // Nullify the asset and animator
                filamentAsset = null
                animator = null
//                Timber.i("filament assete and animator null")
            }
            filamentAsset = null

            // Additional cleanup for the ModelViewer
            modelViewer.scene.skybox?.let { skybox ->
                modelViewer.engine.destroySkybox(skybox)
            }
            modelViewer.scene.indirectLight?.let { indirectLight ->
                modelViewer.engine.destroyIndirectLight(indirectLight)
            }
        }

    }

    public fun cleanup() {
        filamentAsset?.let { asset ->
            // Remove entities from the scene
            modelViewer.scene.removeEntities(asset.entities)

            // Destroy each entity associated with the asset
            asset.entities.forEach { entity ->
                modelViewer.engine.destroyEntity(entity)
            }

            // Nullify the asset and animator
            filamentAsset = null
            animator = null
//            Timber.i("filament assete and animator null")
        }
        filamentAsset = null

        // Additional cleanup for the ModelViewer
        modelViewer.scene.skybox?.let { skybox ->
            modelViewer.engine.destroySkybox(skybox)
        }
        modelViewer.scene.indirectLight?.let { indirectLight ->
            modelViewer.engine.destroyIndirectLight(indirectLight)
        }
    }

    fun onSurfaceAvailable(surfaceView: SurfaceView, lifecycle: Lifecycle) {
        this.surfaceView = surfaceView
        this.lifecycle = lifecycle

        lifecycle.addObserver(lifecycleObserver)

        choreographer = Choreographer.getInstance()
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            isOpaque = false // Make the background transparent
        }

        surfaceView.holder.setFixedSize(surfaceView.width, surfaceView.height)
        surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        modelViewer = ModelViewer(surfaceView = surfaceView)

        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
            true
        }

        modelViewer.scene.skybox = null
        modelViewer.view.blendMode = View.BlendMode.TRANSLUCENT
        modelViewer.renderer.clearOptions = modelViewer.renderer.clearOptions.apply {
            clear = true
        }

        modelViewer.view.apply {
            renderQuality = renderQuality.apply {
                hdrColorBuffer = View.QualityLevel.MEDIUM
            }
        }

        createRenderables()
    }

    private fun createRenderables() {
        val buffer = assets.open("models/untitled.glb").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }

        // Load the model and retrieve the FilamentAsset
        modelViewer.loadModelGlb(buffer)

        filamentAsset = modelViewer.asset // Obtain the FilamentAsset from the ModelViewer

        filamentAsset?.let { asset ->
            ResourceLoader(modelViewer.engine).apply {
                loadResources(asset) // Load resources such as textures and buffers
                destroy() // Destroy the loader after use to free resources
            }


            if(filamentAsset != null){
                // Initialize the animator with the FilamentAsset
                animator = asset.animator
            }


            // Adjust the model to fit within the unit cube
            modelViewer.transformToUnitCube()

            // Start the animation
            startAnimation()
        }
    }

    private fun startAnimation() {
        if(filamentAsset != null ){
            animator?.apply {
                // Check if the asset contains animations
                if (animationCount > 0  && filamentAsset != null) {
                    // Reset start time
                    animationStartTime = System.nanoTime()

                    // Start the first animation (index 0)
                    applyAnimation(0, 0.0f)
                }
            }
        }

    }

    private fun updateAnimation(frameTimeNanos: Long) {
        if(filamentAsset!= null){
            animator?.apply {
                if (animationCount > 0) {
                    // Calculate the elapsed time in seconds
                    val elapsedTimeSeconds = (frameTimeNanos - animationStartTime) / 1_000_000_000.0f

                    // Update the animator with the current time
                    applyAnimation(0, elapsedTimeSeconds)
                }
            }
        }
    }
    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(frameTimeNanos)
            if(filamentAsset != null && animator != null){
                // Update animation frames
                updateAnimation(frameTimeNanos)
            }

        }
    }
}