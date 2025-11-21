package com.example.hackatonproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.hackatonproject.backend.app.runAiAnalysis
import com.example.hackatonproject.backend.upload.sendToNodeServer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageButton
    private var imageCapture: ImageCapture? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 🔦 플래시 / 카메라 객체
    private var camera: Camera? = null
    private var isFlashOn = false

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "Hackathon Project"

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // X 버튼
        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        btnClose.setOnClickListener { finish() }

        // 🔦 플래시 버튼
        val btnFlash = findViewById<ImageButton>(R.id.btnFlash)
        btnFlash.setOnClickListener {
            val cam = camera ?: return@setOnClickListener

            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)

            // 아이콘 바꾸고 싶으면 이렇게
            // btnFlash.setImageResource(if (isFlashOn) R.drawable.design_camera_flash_on
            //                            else R.drawable.design_camera_flash)
        }

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        captureButton.setOnClickListener { takePhoto() }

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // 🔹 bindToLifecycle 결과를 camera에 저장 (플래시 토글용)
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.first(),
            SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.KOREA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "사진 저장 실패: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                @RequiresPermission(
                    allOf = [Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION]
                )
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    getLocationAndSend(photoFile)
                }
            }
        )
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    private fun getLocationAndSend(photoFile: File) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(
                            "CameraActivity",
                            "위치 좌표: lat=${location.latitude}, lon=${location.longitude}"
                        )

                        val geocoder = Geocoder(this@CameraActivity, Locale.KOREA)
                        val addressList =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addressList?.firstOrNull()?.getAddressLine(0) ?: "주소 없음"

                        val aiResult =
                            runAiAnalysis(this@CameraActivity, photoFile.absolutePath)
                        val discomfortType = aiResult.category
                        val confidence = aiResult.confidence
                        Log.d("CameraActivity", "AI 결과: $discomfortType ($confidence)")

                        withContext(Dispatchers.Main) {
                            showConfirmationDialog(
                                photoFile.absolutePath,
                                discomfortType,
                                address
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CameraActivity", "주소 변환 실패", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@CameraActivity,
                                "주소 변환 실패",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showConfirmationDialog(
        photoPath: String,
        discomfortType: String,
        reportLocation: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_confirm, null)

        val tvDiscomfortType = dialogView.findViewById<TextView>(R.id.tvDiscomfortType)
        val tvReportLocation = dialogView.findViewById<TextView>(R.id.tvReportLocation)
        val ivAttachedImage = dialogView.findViewById<ImageView>(R.id.ivAttachedImage)
        val btnPrev = dialogView.findViewById<Button>(R.id.btnPrev)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
        val dialogLoading = dialogView.findViewById<View>(R.id.dialogLoading)

        tvDiscomfortType.text = "불편유형: $discomfortType"
        tvReportLocation.text = "신고위치: $reportLocation"

        val bitmap = BitmapFactory.decodeFile(photoPath)
        ivAttachedImage.setImageBitmap(bitmap)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        dialog.show()

        btnPrev.setOnClickListener {
            dialog.dismiss()
        }

        btnSend.setOnClickListener {
            Log.d("CameraActivity", "🟢 전송 버튼 클릭됨")

            // 다이얼로그 내부 로딩 시작 + 버튼 잠금 (중복 클릭 방지)
            dialogLoading.visibility = View.VISIBLE
            btnSend.isEnabled = false
            btnPrev.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val imageFile = File(photoPath)

                    Log.d(
                        "CameraActivity",
                        "📤 전송 준비: file=${imageFile.absolutePath}, category=$discomfortType, address=$reportLocation"
                    )

                    val result = sendToNodeServer(
                        imageFile = imageFile,
                        predictedCategory = discomfortType,
                        address = reportLocation
                    )

                    withContext(Dispatchers.Main) {
                        // 로딩 종료 + 버튼 복구
                        dialogLoading.visibility = View.GONE
                        btnSend.isEnabled = true
                        btnPrev.isEnabled = true

                        if (result) {
                            Log.d("CameraActivity", "✅ 민원 전송 성공")

                            val imageUri = Uri.fromFile(imageFile)
                            val reportItem =
                                ReportItem(imageUri, reportLocation, discomfortType)
                            ReportRepository.reportList.add(reportItem)

                            val intent =
                                Intent(this@CameraActivity, MainActivity::class.java)
                            intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            startActivity(intent)
                            finish()

                            Toast.makeText(
                                this@CameraActivity,
                                "민원이 정상적으로 접수되었습니다.\n접수된 민원은 나의 민원에서 확인할 수 있습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.w(
                                "CameraActivity",
                                "❌ 민원 전송 실패: 서버에서 false 반환"
                            )

                            Toast.makeText(
                                this@CameraActivity,
                                "민원 전송에 실패했습니다. 인터넷 연결을 확인해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e("CameraActivity", "🔥 전송 중 예외 발생", e)

                    withContext(Dispatchers.Main) {
                        // 로딩 종료 + 버튼 복구
                        dialogLoading.visibility = View.GONE
                        btnSend.isEnabled = true
                        btnPrev.isEnabled = true

                        Toast.makeText(
                            this@CameraActivity,
                            "민원 전송 중 오류가 발생했습니다: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }
    }
}
