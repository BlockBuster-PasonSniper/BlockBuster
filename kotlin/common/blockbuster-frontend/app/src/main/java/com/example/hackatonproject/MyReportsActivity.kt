package com.example.hackatonproject
import MyReportsAdapter
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hackatonproject.R







class MyReportsActivity : AppCompatActivity() {

    private lateinit var myReportsAdapter: MyReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        // 액션바에 뒤로가기 버튼 추가
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "나의 민원"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        myReportsAdapter = MyReportsAdapter(ReportRepository.reportList)
        recyclerView.adapter = myReportsAdapter
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
