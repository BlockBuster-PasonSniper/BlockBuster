import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hackatonproject.R
import com.example.hackatonproject.ReportItem
// 사진 정보를 리스트로 처리하는 공간
class ReportAdapter(private val reports: List<ReportItem>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {
    //각 textview에 불편유형 , 주소 그리고 imageview에 민원사진 썸네일
    inner class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }
    //string 형식으로 받아오는 정보들을 담아두는 공간
    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = reports[position]
        holder.tvCategory.text = "불편유형 : ${item.category}"
        holder.tvAddress.text = "주소 : ${item.address}"
        val bitmap = BitmapFactory.decodeFile(item.photoPath)
        holder.ivPhoto.setImageBitmap(bitmap)
    }


    override fun getItemCount(): Int = reports.size
}
