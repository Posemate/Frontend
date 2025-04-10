package com.example.posee.ui.camera

import android.Manifest
import android.app.Dialog
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
import com.example.posee.R

class CameraActivity : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var interpreter: Interpreter

    private val imageSize = 224
    private val executor = Executors.newSingleThreadExecutor()
    private var latestImageProxy: ImageProxy? = null

    // ✅ 3가지 클래스 이름
    private val classes = arrayOf("proper posture", "wrong posture", "too close")

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
                val bitmap = imageProxyToBitmap(imageProxy)
                val input = preprocess(bitmap)

                val output = Array(1) { FloatArray(3) } // ✅ [1, 3] shape
                interpreter.run(input, output)

                val maxIdx = output[0].indices.maxByOrNull { output[0][it] } ?: -1
                val resultText = classes[maxIdx]

                binding.result.text = resultText
                showResultBubble(resultText)

                imageProxy.close()
                latestImageProxy = null
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
        return BitmapFactory.decodeByteArray(yuvByteArray, 0, yuvByteArray.size)
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
            "proper posture" -> "아저 좋은 자세예요!"
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
