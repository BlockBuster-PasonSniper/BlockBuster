// SimpleImageSender.kt
// 정말 간단한 Kotlin 코드로 RealServer(포트 3000)에 이미지 데이터 전송

import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class SimpleImageSender {
    
    // 서버 URL (실제 서버 IP로 변경하세요)
    private val serverUrl = "http://localhost:3000/api/receive-json"
    // Android 에뮬레이터: "http://10.0.2.2:3000/api/receive-json"
    // 실제 기기: "http://192.168.1.100:3000/api/receive-json"
    
    fun sendImageData(
        title: String,
        description: String,
        imageFileName: String,
        imageDescription: String,
        imageBase64: String
    ): String {
        return try {
            // JSON 데이터 생성
            val jsonData = """
                {
                    "title": "$title",
                    "description": "$description", 
                    "imageFileName": "$imageFileName",
                    "imageDescription": "$imageDescription",
                    "imageData": "$imageBase64"
                }
            """.trimIndent()
            
            // HTTP 연결 설정
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }
            
            // 데이터 전송
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(jsonData.toByteArray())
            outputStream.flush()
            outputStream.close()
            
            // 응답 받기
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            }
            
            connection.disconnect()
            
            "Response Code: $responseCode\nResponse: $response"
            
        } catch (e: IOException) {
            "Error: ${e.message}"
        }
    }
    
    // 파일을 Base64로 변환하는 간단한 함수 (테스트용)
    fun fileToBase64(filePath: String): String {
        return try {
            val fileBytes = java.io.File(filePath).readBytes()
            "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fileBytes)
        } catch (e: Exception) {
            ""
        }
    }
}

// 사용 예제
fun main() {
    val sender = SimpleImageSender()
    
    // 예시 1: 직접 Base64 문자열 사용
    val result1 = sender.sendImageData(
        title = "Kotlin에서 전송한 테스트 이미지",
        description = "간단한 Kotlin 코드로 전송한 이미지입니다.",
        imageFileName = "kotlin_test.jpg",
        imageDescription = "Kotlin 테스트 이미지",
        imageBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A8A"
    )
    println("결과 1:")
    println(result1)
    
    // 예시 2: 파일에서 읽어서 전송 (파일이 있는 경우)
    // val base64Image = sender.fileToBase64("path/to/your/image.jpg")
    // if (base64Image.isNotEmpty()) {
    //     val result2 = sender.sendImageData(
    //         title = "파일에서 읽은 이미지",
    //         description = "실제 파일에서 읽어온 이미지입니다.",
    //         imageFileName = "real_image.jpg",
    //         imageDescription = "실제 이미지 파일",
    //         imageBase64 = base64Image
    //     )
    //     println("결과 2:")
    //     println(result2)
    // }
}
