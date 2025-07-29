import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.hackatonproject.ReportItem
//사진 저장 공간 gson 방식 어지럽다..
object ReportStorage {
    private const val PREF_NAME = "report_pref"
    private const val KEY_REPORT_LIST = "report_list"

    fun saveReport(context: Context, report: ReportItem) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val list = getReports(context).toMutableList()
        list.add(report)
        prefs.edit().putString(KEY_REPORT_LIST, gson.toJson(list)).apply()
    }

    fun getReports(context: Context): List<ReportItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REPORT_LIST, null)
        val type = object : TypeToken<List<ReportItem>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }

    fun clearReports(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_REPORT_LIST).apply()
    }
}
