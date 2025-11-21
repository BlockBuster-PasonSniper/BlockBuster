// 🔁 수정 시작: 패키지 경로를 CameraActivity.kt와 일치시키기 위해 변경
package com.example.hackatonproject.backend.upload
// 🔁 수정 끝

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*


import io.ktor.client.engine.cio.*
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import java.util.Base64
import android.net.Uri
import io.ktor.client.utils.EmptyContent.contentType
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttp

// 🔁 수정 시작: 외부 라우팅 제거 및 내부 호출 방식으로 전환

// AI 분석 실행 함수 (파일과 주소를 입력받아 결과 반환)
data class AiResult(val category: String, val confidence: Float)

//fun runAiAnalysis(imageFile: File, address: String): AiResult {
//    val pythonPath = "python"
//    val scriptPath = File("predict.py").absolutePath
//    val outputPath = File("ai_result.json").absolutePath
//
//    val process = ProcessBuilder(pythonPath, scriptPath, imageFile.absolutePath)
//        .redirectErrorStream(true)
//        .start()
//
//    val stderr = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
//    if (stderr.isNotBlank()) println("[AI stderr] $stderr")
//
//    val json = File(outputPath).readText(Charsets.UTF_8)
//    val parsed = Json.parseToJsonElement(json).jsonObject
//
//    val category = parsed["category"]?.jsonPrimitive?.content ?: "unknown"
//    val confidence = parsed["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.0f
//
//    println("AI 결과: $category ($confidence)")
//    return AiResult(category, confidence)
//}

//fun runAiAnalysis(context: CoroutineScope, imageUri: Uri): Pair<String, Float> {
//    val result = runAiAnalysis(context, imageUri)
//    return Pair(result.category, result.confidence)
//}




// Node.js 서버로 전송
@RequiresApi(Build.VERSION_CODES.O)
suspend fun sendToNodeServer(
    imageFile: File,
    predictedCategory: String,
    address: String,
    name: String = "심여엉",
    telno: String = "010-4444-4444",
    nodeServerUrl: String = "http://192.168.153.145:3005/api/receive-json"
): Boolean {
    return try {
        val base64Image = Base64.getEncoder().encodeToString(imageFile.readBytes())
        val imageDataUri = "data:image/jpeg;base64,$base64Image"

        val payload = buildJsonObject {
            put("category", predictedCategory)
            put("address", address)
            put("name", name)
            put("telno", telno)
            put("pic", imageDataUri)
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post(nodeServerUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        response.status == HttpStatusCode.OK

//        println("Node.js 응다아압: ${response.status}")
//
//        response.status == HttpStatusCode.OK

        val jsonString = Json.encodeToString(JsonObject.serializer(), payload)
        println("전송 Payload: $jsonString")

        // 이 부분은 실제 요청으로 교체 가능 (예: ktor client 등)
        println("Node.js 서버에 JSON 전송 완료 (시뮬레이션)")

        true // 성공 시
    } catch (e: Exception) {
        println("Node.js 전송 실패: ${e.message}")
        false
    }
}

// 🔁 수정 끝
