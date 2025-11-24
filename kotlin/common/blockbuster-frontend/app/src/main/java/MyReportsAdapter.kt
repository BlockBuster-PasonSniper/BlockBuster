import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hackatonproject.R
import com.example.hackatonproject.ReportItem


//민원을 뷰로 적용시키는 파일





class MyReportsAdapter(
    private val reports: MutableList<ReportItem>
) : RecyclerView.Adapter<MyReportsAdapter.ViewHolder>() {


    // adapter의 viewholder를 만들어서  사진 정보들의 id를 이어주고
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.report_image)
        val typeText: TextView = view.findViewById(R.id.report_type)
        val addressText: TextView = view.findViewById(R.id.report_address)
    }



    //item report xml 파일 보면 레이아웃들이 여러 줄 있는데 한 줄씩 끌고오기
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ViewHolder(view)
    }


    //viewHolder의 사진정보를 이용해서 view를 보여줌
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = reports[position]
        holder.imageView.setImageURI(item.imageUri)
        holder.typeText.text = "불편유형: ${item.type}"
        holder.addressText.text = item.address
    }
    override fun getItemCount(): Int = reports.size
    //report에 새 항목 추가하는 메소드
    fun addReport(reportItem: ReportItem) {
        reports.add(reportItem)
        notifyItemInserted(reports.size - 1)
    }

}
