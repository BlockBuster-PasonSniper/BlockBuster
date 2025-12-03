package com.example.hackatonproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.FileProvider
import coil.load
import androidx.camera.core.AspectRatio
import com.example.hackatonproject.backend.app.runAiAnalysis
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
        }

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

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

            // 🔹 프리뷰와 캡처를 동일한 비율(16:9)로 맞춘다
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewView.display.rotation)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewView.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

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

                        // 🔹 AI 분석 호출
                        val aiResult =
                            runAiAnalysis(this@CameraActivity, photoFile.absolutePath)
                        val discomfortType = aiResult.category      // 상위 카테고리 (한글, "기타" 포함)
                        val detailCode = aiResult.detailCode        // D00~D90 또는 ETC
                        val confidence = aiResult.confidence

                        Log.d(
                            "CameraActivity",
                            "AI 결과: code=$detailCode, category=$discomfortType, confidence=$confidence"
                        )
                        Log.d("CameraActivity", "주소 결과: $address")

                        withContext(Dispatchers.Main) {
                            // 🔸 기타인 경우: 다이얼로그는 그대로 띄우되 토스트로만 경고
                            if (discomfortType == "기타") {
                                Toast.makeText(
                                    this@CameraActivity,
                                    "AI가 사진을 정확히 인식하지 못했습니다.\n불편유형이 '기타'로 설정됩니다.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            // 🔸 불편유형이 무엇이든 항상 확인 다이얼로그로 진행
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
                Toast.makeText(this, "위치를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
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

        // 사진 촬영 이후 가로로 dialog 나오는 문제 -> bitmap 방식이 아닌 Coil 도입
        ivAttachedImage.load(File(photoPath))

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

            // ✅ Node 서버 사용 없이, 바로 이메일 앱 + 로컬 리스트 추가
            val imageFile = File(photoPath)

            Log.d(
                "CameraActivity",
                "📤 전송 준비(이메일): file=${imageFile.absolutePath}, category=$discomfortType, address=$reportLocation"
            )

            // 리포트 목록에 추가 (앱 내 히스토리용)
            val imageUri = Uri.fromFile(imageFile)
            val reportItem = ReportItem(imageUri, reportLocation, discomfortType)
            ReportRepository.reportList.add(reportItem)

            // 이메일 앱 열기 (사진 첨부 포함)
            val cityHallEmail = "honeyfog00@gmail.com" // TODO: 실제 민원 담당 이메일로 교체
            sendReportEmail(
                toEmail = cityHallEmail,
                address = reportLocation,
                category = discomfortType,
                imageFile = imageFile
            )

            // 로딩 종료 + 버튼 복구
            dialogLoading.visibility = View.GONE
            btnSend.isEnabled = true
            btnPrev.isEnabled = true

            Toast.makeText(
                this@CameraActivity,
                "이메일 화면에서 전송 버튼을 눌러주세요.",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
            // ★ CameraActivity 닫기
            finish()
        }
    }

    private fun sendReportEmail(
        toEmail: String,
        address: String,
        category: String,
        imageFile: File
    ) {
        val subject = "[도로이용불편 신고]"
        val body = """
        [도로이용불편 신고]

        위치: $address
        유형: $category
    """.trimIndent()

        val imageUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )

        Log.d("CameraActivity", "📧 sendReportEmail 호출됨, to=$toEmail, uri=$imageUri")

        val intent = Intent(Intent.ACTION_SEND).apply {
            // 메일 클라이언트를 우선 대상으로
            type = "message/rfc822"

            putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, imageUri)

            // 다른 앱(메일 앱)이 이 URI를 읽을 수 있도록 권한 부여
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 이메일 앱만 선택되도록 유도
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "이메일 앱을 선택하세요"))
        } else {
            Toast.makeText(this, "이메일 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
