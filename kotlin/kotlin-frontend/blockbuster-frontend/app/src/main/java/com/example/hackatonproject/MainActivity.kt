package com.example.hackatonproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnMyComplaints = findViewById<Button>(R.id.btnMyComplaints)

        btnTakePhoto.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
        btnMyComplaints.setOnClickListener {
            val intent = Intent(this, WebviewActivity::class.java)
            intent.putExtra("url", "http://localhost:3000/api/receive-json")
            startActivity(intent)
        }


    }
}


