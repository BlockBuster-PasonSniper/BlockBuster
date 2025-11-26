package com.example.hackatonproject

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyReportsActivity : AppCompatActivity() {

    private lateinit var myReportsAdapter: MyReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        // 🔙 커스텀 뒤로가기 버튼 (레이아웃의 btnBack)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()   // 현재 액티비티 종료 → 이전 화면으로
        }

        // 🔽 RecyclerView 세팅
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        myReportsAdapter = MyReportsAdapter(ReportRepository.reportList)
        recyclerView.adapter = myReportsAdapter
    }

    override fun onResume() {
        super.onResume()
        // 카메라에서 새 민원 추가 후 돌아왔을 때 리스트 갱신
        myReportsAdapter.notifyDataSetChanged()
    }
}
