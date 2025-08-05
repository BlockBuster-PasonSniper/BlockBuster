package com.example.hackatonproject


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
//import org.tensorflow.lite.TensorFlowLite

class MainActivity : AppCompatActivity() {

//    val ver = TensorFlowLite.runtimeVersion()
//    val schema = TensorFlowLite.schemaVersion()  // 참고: 스키마 버전
//    Log.i("TFLite", "runtime=$ver, schema=$schema")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnMyComplaints = findViewById<Button>(R.id.btnMyComplaints)
        //사진촬영 버튼 이벤트
        btnTakePhoto.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
        //나의민원 버튼 이벤트
        btnMyComplaints.setOnClickListener {
            val intent = Intent(this, MyReportsActivity::class.java)
            startActivity(intent)
        }



    }
}


