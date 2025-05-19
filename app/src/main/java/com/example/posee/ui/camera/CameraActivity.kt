package com.example.posee.ui.camera

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Size
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.posee.databinding.FragmentCameraBinding
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.example.posee.R
import com.example.posee.network.AlarmLogRequest
import com.example.posee.network.RetrofitClient
import retrofit2.Call
import java.time.Instant
import java.time.format.DateTimeFormatter

class CameraActivity : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var interpreter: Interpreter

    private val imageSize = 224
    private val executor = Executors.newSingleThreadExecutor()
    private var latestImageProxy: ImageProxy? = null

    private val classes = arrayOf("proper posture", "wrong posture", "too close")
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setGuideTextStyle() {
        val text = "버튼을 누르어\n올바른 자세인지 아니지를\n확인할 수 있어요"
        val spannable = SpannableString(text)

        val purple = ContextCompat.getColor(requireContext(), R.color.purple_500)
        spannable.setSpan(ForegroundColorSpan(purple), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(purple), 7, 17, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.guideText.text = spannable
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGuideTextStyle()

        val prefsId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = prefsId.getString("logged_in_userId", null)
            ?: throw IllegalStateException("로그인된 사용자 ID가 없습니다.")

        val prefs = requireContext().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        val backgroundEnabled = prefs.getBoolean("background_switch_state", false)

        if (backgroundEnabled) {
            val intent = Intent(requireContext(), PoseDetectionService::class.java)
            ContextCompat.startForegroundService(requireContext(), intent)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA), 100
            )
        }

        interpreter = Interpreter(loadModelFile("model.tflite"))

        binding.guideCover.setOnClickListener {
            binding.guideCover.visibility = View.GONE
            binding.guideText.visibility = View.GONE
            binding.guideLine.visibility = View.GONE
        }

        binding.btnAnalyze.setOnClickListener {
            latestImageProxy?.let { imageProxy ->
                // 최신 프레임을 즉시 bitmap으로 변환
                val bitmap = imageProxyToBitmap(imageProxy)
                // 프레임 닫고 null 처리 (중복 분석 방지)
                imageProxy.close()
                latestImageProxy = null

                val input = preprocess(bitmap)
                val output = Array(1) { FloatArray(3) }
                interpreter.run(input, output)

                val maxProb = output[0].maxOrNull() ?: 0f
                val maxIdx = output[0].indices.maxByOrNull { output[0][it] } ?: -1

                if (maxProb > 0.6f) {
                    val resultText = classes[maxIdx]

                    val nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    val request = AlarmLogRequest(
                        userId = userId,
                        alarmTime = nowIso,
                        postureType = maxIdx + 1
                    )
                    RetrofitClient.apiService().postAlarmLog(request)
                        .enqueue(object : retrofit2.Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {}
                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Log.e("CameraActivity", "AlarmLog POST failed: ${t.message}")
                            }
                        })

                    showResultBubble(resultText)
                } else {
                    Log.d("Posee", "불확실한 결과이므로 알림 생략: ${output[0].joinToString()}")
                }
            } ?: run {
                Toast.makeText(requireContext(), "카메라 프레임을 가져오는 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(imageSize, imageSize))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                latestImageProxy?.close()
                latestImageProxy = imageProxy
            })

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val yuvByteArray = out.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(yuvByteArray, 0, yuvByteArray.size)

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f) // 전면 카메라 좌우 반전
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        scaled.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)
        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255f)
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255f)
                byteBuffer.putFloat((value and 0xFF) / 255f)
            }
        }
        return byteBuffer
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val fileDescriptor = requireContext().assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun showResultBubble(result: String) {
        val message = when (result) {
            "proper posture" -> "아주 좋은 자세예요!"
            "wrong posture" -> "자세를 조금만 고쳐볼까요!"
            "too close" -> "너무 가까워요!"
            else -> "결과를 알 수 없습니다."
        }

        val dialog = Dialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_result_bubble, null)

        val messageText = view.findViewById<TextView>(R.id.messageText)
        val appNameText = view.findViewById<TextView>(R.id.appName)
        val timeText = view.findViewById<TextView>(R.id.timeText)
        val closeBtn = view.findViewById<TextView>(R.id.closeBtn)

        messageText.text = message
        appNameText.text = "Posee"
        timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)

        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val params = window.attributes
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 150
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            window.attributes = params
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        interpreter.close()
    }
}
