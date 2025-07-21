package com.example.hackatonproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


//카메라 촬영을 위한 activity
class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private var imageCapture: ImageCapture? = null
    //권한 확인하기
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
//앱 시작하면 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    //ui로 사용하는 activity_camera
        setContentView(R.layout.activity_camera)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//미리보기 뷰와 버튼 연결
        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
//촬영 버튼-> takePhoto 불러옴
        captureButton.setOnClickListener {
            takePhoto()
        }
//권한 o -> 실행 , x-> permission.camera로 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d("CameraActivity", "뒤로가기 클릭")
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
//미리보기 시작하는 함수
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
                cameraProvider.bindToLifecycle(
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
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.KOREA)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//여기서 사진 저장하는 형식을 고치면 됩니다-> ai가 분석한 json 형식을 받아 아마 key 형식으로 넣으면 되지 않을까 싶은데
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "사진 저장 실패: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    showConfirmationDialog(photoFile.absolutePath, "배수 시설 불량", "충청북도 청주시 상당구 용암동 1470")

                }
            }
        )
    }
    //팝업 부분은 제가 언제든지 수정 가능합니다 수정해야 할 부분-> photopath , type , location ..etc
    private fun showConfirmationDialog(photoPath: String, discomfortType: String, reportLocation: String) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_confirm, null)

        val tvDiscomfortType = dialogView.findViewById<TextView>(R.id.tvDiscomfortType)
        val tvReportLocation = dialogView.findViewById<TextView>(R.id.tvReportLocation)
        val ivAttachedImage = dialogView.findViewById<ImageView>(R.id.ivAttachedImage)
        val btnPrev = dialogView.findViewById<Button>(R.id.btnPrev)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)

        tvDiscomfortType.text = "불편유형: $discomfortType"
        tvReportLocation.text = "신고위치: $reportLocation"

        val bitmap = BitmapFactory.decodeFile(photoPath)
        ivAttachedImage.setImageBitmap(bitmap)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnPrev.setOnClickListener {
            dialog.dismiss()
        }
//민원처리 후 main 페이지로 이동 및 toast로 text 생성
        btnSend.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
            Toast.makeText(this, "민원이 정상적으로 접수되었습니다. \n나의 민원을 통해 확인할 수 있습니다.",
                Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

}
