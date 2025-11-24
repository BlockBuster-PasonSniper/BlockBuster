package routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

import java.io.File
import java.util.*

var latestUploadedFile: File? = null
var latestAddress: String? = null

fun Route.uploadRoutes() {
    post("/upload") {
        try {
            println("▶ [UPLOAD] 요청 수신됨")

            val contentType = call.request.contentType()
            if (!contentType.match(ContentType.MultiPart.FormData)) {
                call.respond(HttpStatusCode.BadRequest, "Content-Type은 multipart/form-data 여야 합니다.")
                return@post
            }

            val multipart = call.receiveMultipart()
            val uploadDir = File("uploads")
            if (!uploadDir.exists()) uploadDir.mkdirs()

            var savedFile: File? = null
            var receivedAddress: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val ext = File(part.originalFileName ?: "image.jpg").extension
                        val fileName = UUID.randomUUID().toString() + ".$ext"
                        savedFile = File(uploadDir, fileName)
                        part.streamProvider().use { input ->
                            savedFile!!.outputStream().use { output -> input.copyTo(output) }
                        }
                        println("✔ 이미지 저장 완료: ${savedFile!!.absolutePath}")
                    }

                    is PartData.FormItem -> {
                        if (part.name == "address") {
                            receivedAddress = part.value
                            println("✔ 주소 수신: $receivedAddress")
                        }
                    }

                    else -> Unit
                }
                part.dispose()
            }

            if (savedFile == null) {
                call.respond(HttpStatusCode.BadRequest, "no file")
                return@post
            }

            latestUploadedFile = savedFile
            latestAddress = receivedAddress

            // ✅ AI 분석 실행
            val (predictedCategory, confidence) = runAiAnalysis(savedFile!!.absolutePath)

            // ✅ 프론트에 리턴할 JSON 구성 (이미지 base64 제외)
            val jsonResponse = buildJsonObject {
                put("category", predictedCategory)
                put("confidence", confidence)
                put("address", receivedAddress ?: "주소 없음")
                put("name", "김계계")
                put("telno", "010-4444-4444")
                // 이미지 base64나 pic은 아직 전송하지 않음
            }

            println("📤 프론트로 분석 결과 전송: $jsonResponse")
            call.respondText(
                    jsonResponse.toString(),
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    HttpStatusCode.OK
)

        } catch (e: Exception) {
            println("❌ error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, "server error")
        }
    }
     post("/signal") {
        try {
            println("📶 signul recive")

            if (latestUploadedFile == null || latestAddress == null) {
                call.respond(HttpStatusCode.BadRequest, "최근 업로드 파일 또는 주소가 없습니다.")
                return@post
            }

            val (predictedCategory, confidence) = runAiAnalysis(latestUploadedFile!!.absolutePath)

            val success = sendToNodeServer(
                imageFile = latestUploadedFile!!,
                predictedCategory = predictedCategory,
                address = latestAddress!!
            )

            if (success) {
                println("✅ Node.js 전송 성공")
                call.respond(HttpStatusCode.OK, "전송 완료")
            } else {
                println("❌ Node.js 전송 실패")
                call.respond(HttpStatusCode.InternalServerError, "Node.js 전송 실패")
            }
        } catch (e: Exception) {
            println("❌ 시그널 처리 오류: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, "서버 오류 발생")
        }
    }
}



// AI 분석 실행 함수
fun runAiAnalysis(imagePath: String): Pair<String, Float> {
    val pythonPath = "python"
    val scriptPath = File("predict.py").absolutePath
    val outputPath = File("ai_result.json").absolutePath

    val process = ProcessBuilder(pythonPath, scriptPath, imagePath, outputPath)
        .redirectErrorStream(true)
        .start()

    val stderr = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
    if (stderr.isNotBlank()) println("[AI stderr] $stderr")

    val json = File(outputPath).readText(Charsets.UTF_8)
    val parsed = Json.parseToJsonElement(json).jsonObject

    val category = parsed["category"]?.jsonPrimitive?.content ?: "unknown"
    val confidence = parsed["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.0f

    println("AI 결과: $category ($confidence)")
    return Pair(category, confidence)
}

// Node.js 서버로 전송
suspend fun sendToNodeServer(
    imageFile: File,
    predictedCategory: String,
    address: String,
    nodeServerUrl: String = "http://localhost:3000/api/receive-json"
): Boolean {
    return try {
        val base64Image = Base64.getEncoder().encodeToString(imageFile.readBytes())
        val imageDataUri = "data:image/jpeg;base64,$base64Image"

        val payload = buildJsonObject {
            put("category", predictedCategory)               // AI 분석 결과
            put("address", address)                          // 프론트에서 전달받은 주소
            put("name", "김계계")                             // 고정값 (추후 로그인 정보로 대체 가능)
            put("telno", "010-4444-4444")                    // 고정값
            put("pic", imageDataUri)                         // 인코딩된 이미지
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
       
        println("Node.js 응다아압: ${response.status}")
        
        response.status == HttpStatusCode.OK
        

    } catch (e: Exception) {
        println("Node.js 전송 실패: ${e.message}")
        false
    }
}
