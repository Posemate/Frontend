package com.example.posee.ui.camera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.posee.databinding.FragmentCameraBinding
import com.example.posee.ml.Model
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val imageSize = 224

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultData ->
            if (resultData.resultCode == Activity.RESULT_OK) {
                val image = resultData.data?.extras?.get("data") as? Bitmap
                if (image == null) {
                    Toast.makeText(requireContext(), "사진을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                val dimension = minOf(image.width, image.height)
                val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                binding.imageView.setImageBitmap(thumbnail)

                val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                classifyImage(scaledImage)
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

        // 뒤로가기 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        launchCameraIfPermitted()
    }

    private fun launchCameraIfPermitted() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        } else {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
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

            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
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
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val confidences = outputFeature0.floatArray
            val classes = arrayOf("proper posture", "wrong posture")

            val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: -1
            val resultText = classes.getOrNull(maxIndex) ?: "Unknown"
            Log.d("PostureResult", "예측 결과: $resultText")

            binding.result.text = resultText

            val s = StringBuilder()
            for (i in classes.indices) {
                s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100))
            }
            binding.confidence.text = s.toString()

            model.close()

            showResultBottomSheet(resultText)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "모델 실행 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResultBottomSheet(result: String) {
        val message = when (result) {
            "proper posture" -> "✅ 바른 자세예요! 😊"
            "wrong posture" -> "❗ 자세를 고쳐 앉으세요! 🪑"
            else -> "❓ 결과를 알 수 없습니다. 다시 시도해주세요."
        }

        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val text = view.findViewById<TextView>(android.R.id.text1)
        text.text = message
        text.textSize = 18f
        text.setPadding(40, 60, 40, 60)

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
