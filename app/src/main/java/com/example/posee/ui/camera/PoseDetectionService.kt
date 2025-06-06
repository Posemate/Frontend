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
import java.time.Instant
import java.time.format.DateTimeFormatter
import com.example.posee.network.AlarmLogRequest
import com.example.posee.network.RetrofitClient
import retrofit2.Call
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import android.app.ActivityManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.LayoutInflater
import android.view.Gravity
import android.widget.Toast


class PoseDetectionService : Service() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var interpreter: Interpreter
    private val imageSize = 224
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var userId: String
    private var lastNotificationTime = 0L

    private val lifecycleOwner = DummyLifecycle()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analyzer: ImageAnalysis? = null

    // 다른 앱 위에 창 띄우기
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var overlayParams: WindowManager.LayoutParams

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // 다른 앱 위에 창 띄우기
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 2-2. floatingView Inflate (layout/floating_overlay.xml 을 미리 만들어두세요)
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_overlay, null)

        // 2-3. overlayParams 설정
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // 초기 위치(원하는 값으로 조정)
        overlayParams.gravity = Gravity.TOP or Gravity.START
        overlayParams.x = 100
        overlayParams.y = 200


        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getString("logged_in_userId", null)
            ?: throw IllegalStateException("로그인된 사용자 ID가 없습니다.")

        interpreter = Interpreter(loadModelFile("model.tflite"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 알림 표시 (5초 내 필수)
        startForegroundWithNotification("자세 분석 중입니다.")

        // 2. 권한 확인 후 카메라 분석 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraAnalysis()
        } else {
            stopSelf() // 권한 없으면 서비스 중단
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        cameraProvider?.unbindAll()
        executor.shutdown()
    }

    private fun shouldNotify(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastNotificationTime > 10000) {
            lastNotificationTime = currentTime
            true
        } else {
            false
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    private fun startForegroundWithNotification(message: String) {
        val intent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "pose_popup_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "자세 팝업 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
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
                val overlay = prefs.getBoolean("overlay_switch_state", false)

                if ((result == "wrong posture" && neckOn) || (result == "too close" && eyeOn)) {
                    if (shouldNotify() && !isAppInForeground()) {  // 앱이 포그라운드 아니면 알림 띄움
                        val message = when (result) {
                            "wrong posture" -> "자세가 좋지 않아요!"
                            "too close" -> "너무 가까워요!"
                            else -> return@Analyzer
                        }
                        startForegroundWithNotification(message)
                        if (overlay) {
                            // 메인 스레드로 전환하여 오버레이 추가/제거 작업 수행
                            mainHandler.post {
                                // 1) 오버레이 권한 확인
                                if (Settings.canDrawOverlays(this)) {
                                    // 2) 아직 뷰가 붙어 있지 않으면 addView 실행
                                    if (floatingView.parent == null) {
                                        windowManager.addView(floatingView, overlayParams)
                                    }
                                    // 3) 2초 뒤에 메인 스레드에서 removeView 실행
                                    mainHandler.postDelayed({
                                        if (floatingView.parent != null) {
                                            windowManager.removeView(floatingView)
                                        }
                                    }, 2000L)
                                } else {
                                    // 권한이 없으면 사용자에게 알림(Toast 등) – 역시 메인 스레드에서
                                    Toast.makeText(
                                        this,
                                        "오버레이 권한이 필요합니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        val nowKst = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        val alarmTime = nowKst.format(formatter)
                        val postureType = when (result) {
                            "wrong posture" -> 2
                            "too close"     -> 3
                            else            -> 1
                        }
                        RetrofitClient
                            .apiService()
                            .postAlarmLog( AlarmLogRequest(userId, alarmTime, postureType) )
                            .enqueue(object : retrofit2.Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                                    // 성공 시 별도 처리 없음
                                }
                                override fun onFailure(call: Call<Void>, t: Throwable) {
                                    Log.e("PoseDetectionService", "AlarmLog POST failed: ${t.message}")
                                }
                            })
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
