package routes

// Ktor 기본 요소 import
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*

// 파일 관련 기능
import java.io.File
import java.util.UUID

// AI 예측 수행을 위한 외부 Python 스크립트 호출 함수
fun runAiPrediction(imagePath: String): String {
    // 시스템에 설치된 Python 실행 경로
    val pythonPath = "python" // 또는 절대 경로: "C:\\Python311\\python.exe"

    // 현재 프로젝트 루트에 위치한 predict.py 파일 경로
    val scriptPath = File("predict.py").absolutePath

    // 이미지 파일의 전체 경로
    val imageFilePath = imagePath

    println("▶ [AI] 실행 명령: $pythonPath $scriptPath $imageFilePath")

    // ProcessBuilder로 외부 Python 스크립트를 실행
    val process = ProcessBuilder(pythonPath, scriptPath, imageFilePath)
        .redirectErrorStream(true)
        .start()

    // 결과값(표준 출력) 수신
    val result = process.inputStream.bufferedReader(Charsets.UTF_8).readText()

    println("▶ [AI] 응답 결과: $result")

    return result
}

// 실제 라우팅 처리 함수 정의
fun Route.uploadRoutes() {
    post("/upload") {
        try {
            println("▶ [UPLOAD] 요청 수신됨")
            val contentType = call.request.contentType()

        if (!contentType.match(ContentType.MultiPart.FormData)) {
            println("❗ [UPLOAD] Content-Type 오류: $contentType")
            call.respond(HttpStatusCode.BadRequest, "Content-Type은 multipart/form-data 여야 합니다.")
            return@post
        }

            // multipart/form-data 형식 수신
            val multipart = call.receiveMultipart()
            println("▶ [UPLOAD] multipart 데이터 수신 완료")

            // 업로드 폴더 확인 및 없으면 생성
            val uploadDir = File("uploads")
            if (!uploadDir.exists()) {
                println("▶ [UPLOAD] uploads 폴더 생성")
                uploadDir.mkdirs()
            }

            var savedFile: File? = null

            // multipart 내부 파트 처리
            multipart.forEachPart { part ->
                println("▶ [UPLOAD] 파트 처리 중: ${part::class.simpleName}")

                if (part is PartData.FileItem) {
                    // 파일 확장자 및 저장 이름 지정
                    val fileExt = File(part.originalFileName ?: "image.jpg").extension
                    val fileName = UUID.randomUUID().toString() + ".$fileExt"
                    savedFile = File(uploadDir, fileName)

                    // 파일 저장
                    part.streamProvider().use { input ->
                        savedFile!!.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    println("✅ [UPLOAD] 파일 저장 완료: ${savedFile!!.absolutePath}")
                }

                // 자원 정리
                part.dispose()
            }

            // 파일 저장 실패 시 예외 처리
            if (savedFile == null) {
                println("❗ [UPLOAD] 저장된 파일 없음")
                call.respond(HttpStatusCode.BadRequest, "파일이 없습니다.")
                return@post
            }

            // 🔁 AI 분석 실행
            val resultJson = runAiPrediction(savedFile!!.absolutePath)

            // JSON 형식으로 응답 반환
            call.respondText(resultJson, ContentType.Application.Json)

        } catch (e: Exception) {
            println("❗ [UPLOAD] 예외 발생: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "서버 오류 발생")
        }
    }
}
