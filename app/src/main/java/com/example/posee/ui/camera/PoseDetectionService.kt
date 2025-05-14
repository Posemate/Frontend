package com.example.posee.ui.camera

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.os.Build
import android.os.IBinder
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.posee.R
import com.google.common.util.concurrent.ListenableFuture
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class PoseDetectionService : Service() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var interpreter: Interpreter
    private val imageSize = 224
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var userId: String

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // SharedPreferences에서 userId 로드
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = prefs.getString("logged_in_userId", null)
            ?: throw IllegalStateException("로그인된 사용자 ID가 없습니다.")
        interpreter = Interpreter(loadModelFile("model.tflite"))
        startForegroundWithNotification()
        startCameraAnalysis()
    }

    private fun startForegroundWithNotification() {
        val channelId = "pose_channel"
        val channelName = "Pose Detection Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Posee 자세 분석 중")
            .setContentText("앱이 백그라운드에서도 실행 중입니다.")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }

    private fun startCameraAnalysis() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(imageSize, imageSize))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                val bitmap = imageProxyToBitmap(imageProxy)
                val input = preprocess(bitmap)
                val output = Array(1) { FloatArray(3) }
                interpreter.run(input, output)

                val result = when (output[0].indices.maxByOrNull { output[0][it] }) {
                    1 -> "wrong posture"
                    2 -> "too close"
                    else -> null
                }

                // SharedPreferences에서 알림 설정 상태 불러오기
                val prefs = getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
                val neckOn = prefs.getBoolean("neck_switch_state", false)
                val eyeOn = prefs.getBoolean("eye_switch_state", false)

                // 조건에 따라 알림 전송
                if ((result == "wrong posture" && neckOn) || (result == "too close" && eyeOn)) {
                    sendNotification(result)

                    //  백그라운드에서는 로그 저장하지 않음
                    // 로그는 CameraActivity 버튼 클릭 시에만 저장됨
                }

                imageProxy.close()
            })

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.bindToLifecycle(DummyLifecycle(), cameraSelector, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun sendNotification(result: String) {
        val message = when (result) {
            "wrong posture" -> "자세가 좋지 않아요!"
            "too close" -> "너무 가까워요!"
            else -> return
        }

        val notification = NotificationCompat.Builder(this, "pose_channel")
            .setContentTitle("자세 알림")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(2, notification)
            }
        } else {
            NotificationManagerCompat.from(this).notify(2, notification)
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val nv21 = ByteArray(yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining())
        yBuffer.get(nv21, 0, yBuffer.remaining())
        vBuffer.get(nv21, yBuffer.remaining(), vBuffer.remaining())
        uBuffer.get(nv21, yBuffer.remaining() + vBuffer.remaining(), uBuffer.remaining())

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
        val buffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(imageSize * imageSize)
        scaled.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
        for (value in pixels) {
            buffer.putFloat(((value shr 16) and 0xFF) / 255f)
            buffer.putFloat(((value shr 8) and 0xFF) / 255f)
            buffer.putFloat((value and 0xFF) / 255f)
        }
        return buffer
    }

    class DummyLifecycle : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle
            get() = lifecycleRegistry.apply {
                currentState = Lifecycle.State.STARTED
            }
    }
}
