package com.example.posee.ui.camera
import android.app.Dialog
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.posee.R
import com.example.posee.databinding.FragmentCameraBinding
import com.example.posee.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val imageSize = 224
    private var shouldLaunchCamera = true

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultData ->
            if (resultData.resultCode == android.app.Activity.RESULT_OK) {
                val image = resultData.data?.extras?.get("data") as? Bitmap
                image?.let {
                    val dimension = minOf(it.width, it.height)
                    val thumbnail = ThumbnailUtils.extractThumbnail(it, dimension, dimension)
                    binding.imageView.setImageBitmap(thumbnail)

                    val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                    classifyImage(scaledImage)
                }
            } else {
                Toast.makeText(requireContext(), "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (shouldLaunchCamera) {
            launchCamera()
            shouldLaunchCamera = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageView.setOnClickListener {
            shouldLaunchCamera = true
            launchCamera()
        }
    }


    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        AlertDialog.Builder(requireContext())
            .setTitle("카메라 권한 필요")
            .setMessage("사진을 찍으려면 카메라 권한이 필요합니다.\n설정 > 앱 권한에서 허용해주세요.")
            .setPositiveButton("권한 요청") { _, _ ->
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    100
                )
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val model = Model.newInstance(requireContext())

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) / 255f)
                    byteBuffer.putFloat(((value shr 8) and 0xFF) / 255f)
                    byteBuffer.putFloat((value and 0xFF) / 255f)
                }
            }

            inputFeature0.loadBuffer(byteBuffer)
            val outputs = model.process(inputFeature0)
            val confidences = outputs.outputFeature0AsTensorBuffer.floatArray
            val classes = arrayOf("proper posture", "wrong posture")

            val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1
            val resultText = classes.getOrNull(maxIndex) ?: "Unknown"

            binding.result.text = resultText

            val s = StringBuilder()
            for (i in classes.indices) {
                s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100))
            }
            binding.confidence.text = s.toString()

            model.close()

            showResultBubble(resultText)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "모델 실행 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResultBubble(result: String) {
        val message = when (result) {
            "proper posture" -> "바른 자세예요!"
            "wrong posture" -> "화면과 너무 가까워요!"
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

        // 현재 시간 설정
        val currentTime = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = formatter.format(currentTime)

        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)

        // 다이얼로그 위치 설정 (상단 알림처럼)
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
    }

}
