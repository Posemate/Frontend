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
import java.util.concurrent.Executors

class PoseDetectionService : Service() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var interpreter: Interpreter
    private val imageSize = 224
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var userId: String
    private var lastNotificationTime = 0L

    // DummyLifecycle를 멤버 변수로 만들어서 서비스 종료 시 바인딩 해제 가능하도록
    private val lifecycleOwner = DummyLifecycle()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analyzer: ImageAnalysis? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getString("logged_in_userId", null)
            ?: throw IllegalStateException("로그인된 사용자 ID가 없습니다.")
        interpreter = Interpreter(loadModelFile("model.tflite"))
        startCameraAnalysis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스가 강제 종료되어도 자동 재시작 하지 않도록
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 알림 제거 및 포그라운드 서비스 종료
        stopForeground(true)
        // 카메라 바인딩 해제
        cameraProvider?.unbindAll()
        executor.shutdown()
    }

    private fun shouldNotify(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastNotificationTime > 10000) { // 10초 간격 제한
            lastNotificationTime = currentTime
            true
        } else {
            false
        }
    }

    private fun startForegroundWithNotification(message: String) {
        val intent = Intent(this, AlertActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "pose_popup_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "자세 팝업 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Posee")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo_popup)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
            startForeground(notificationId, notification)
        }
    }

    private fun startCameraAnalysis() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(imageSize, imageSize))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer?.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                val bitmap = imageProxyToBitmap(imageProxy)
                val input = preprocess(bitmap)
                val output = Array(1) { FloatArray(3) }
                interpreter.run(input, output)

                val result = when (output[0].indices.maxByOrNull { output[0][it] }) {
                    1 -> "wrong posture"
                    2 -> "too close"
                    else -> null
                }

                val prefs = getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
                val neckOn = prefs.getBoolean("neck_switch_state", false)
                val eyeOn = prefs.getBoolean("eye_switch_state", false)

                if ((result == "wrong posture" && neckOn) || (result == "too close" && eyeOn)) {
                    if (shouldNotify()) {
                        val message = when (result) {
                            "wrong posture" -> "자세가 좋지 않아요!"
                            "too close" -> "너무 가까워요!"
                            else -> return@Analyzer
                        }
                        startForegroundWithNotification(message)
                    }
                }

                imageProxy.close()
            })

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, analyzer)
        }, ContextCompat.getMainExecutor(this))
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
