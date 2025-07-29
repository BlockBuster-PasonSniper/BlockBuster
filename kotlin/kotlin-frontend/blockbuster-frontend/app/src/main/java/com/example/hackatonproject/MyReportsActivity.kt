package com.example.hackatonproject
import ReportAdapter
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hackatonproject.R

class MyReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        // 액션바에 뒤로가기 버튼 추가
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "홈으로"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewReports)
        val reports = ReportStorage.getReports(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ReportAdapter(reports)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // 뒤로가기 버튼 누르면 액티비티 종료
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
