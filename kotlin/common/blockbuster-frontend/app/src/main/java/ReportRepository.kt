import com.example.hackatonproject.ReportItem


// 일단 본선에서는 로컬에서 RAM 메모리에 저장하고 결선가면 DB 써서 영구 저장을 하는 방식으로 가면 될 것 같습니다
// ReportRepository는  ReportItem 정보들이 저장되는 공간
object ReportRepository {
    val reportList = mutableListOf<ReportItem>()
}
