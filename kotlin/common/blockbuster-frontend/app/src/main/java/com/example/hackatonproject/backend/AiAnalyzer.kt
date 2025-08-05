import kotlinx.serialization.json.Json
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

// AI 분석 실행 함수
fun runAiAnalysis(imagePath: String): Pair<String, Float> {
    val pythonPath = "python"  // Windows 환경에서는 python 또는 python3로 설정
    val scriptPath = File("predict.py").absolutePath
    val outputPath = File("ai_result.json").absolutePath

    val process = ProcessBuilder(pythonPath, scriptPath, imagePath, outputPath)
        .redirectErrorStream(true)  // stderr도 함께 출력
        .start()

    val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
    if (output.isNotBlank()) {
        println("[AI output] $output")  // stdout 로그 확인용
    }

    process.waitFor()

    val resultFile = File(outputPath)
    if (!resultFile.exists()) {
        println("AI 결과 파일이 존재하지 않음")
        return Pair("unknown", 0.0f)
    }

    val json = resultFile.readText(Charsets.UTF_8)
    val parsed = Json.parseToJsonElement(json).jsonObject

    val category = parsed["category"]?.jsonPrimitive?.content ?: "unknown"
    val confidence = parsed["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.0f

    println("AI 분석 결과: $category ($confidence)")
    return Pair(category, confidence)
}
