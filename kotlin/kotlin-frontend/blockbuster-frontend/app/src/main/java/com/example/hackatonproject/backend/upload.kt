// Node.js(또는 백엔드)로 민원 정보를 보내는(척하는) 유틸 함수
// 실제 대회/시연에서는 JSON을 로그로만 남기고 항상 true를 리턴하도록 구성

package com.example.hackatonproject.backend.upload

import android.util.Base64
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicLong

// 간단한 인덱스 생성기 (id 1, 2, 3, ...)
private val idGenerator = AtomicLong(1L)

/**
 * 이미지 + AI 분류 결과 + 주소를 JSON 형태로 만들어
 * "서버로 전송했다" 고 가정하는 함수.
 *
 * - 실제 HTTP 요청은 아직 넣지 않고
 *   Logcat에 JSON만 찍은 뒤 true 반환
 *
 * - 나중에 진짜 서버 연동 시, 이 함수 안에서
 *   OkHttp / Ktor Client로 POST 요청만 추가하면 됨.
 */
fun sendToNodeServer(
    imageFile: File,
    predictedCategory: String,
    address: String
): Boolean {
    return try {
        // 1) 이미지 → Base64 인코딩
        val imageBytes = imageFile.readBytes()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // 2) 민원 고유 ID 생성 (0001, 0002 느낌)
        val id = idGenerator.getAndIncrement()

        // 3) JSON 문자열 하드코딩 생성
        //    (kotlinx-serialization 안 쓰고 단순 문자열로 처리)
        val json = """
            {
              "id": $id,
              "category": "${escapeJson(predictedCategory)}",
              "address": "${escapeJson(address)}",
              "imageBase64": "$base64Image",
              "createdAt": ${System.currentTimeMillis()}
            }
        """.trimIndent()

        // 4) 일단은 로그에만 찍고 "전송 성공"으로 처리
        Log.d("sendToNodeServer", "전송할 민원 JSON = $json")

        // TODO: 진짜 서버 연동하고 싶으면 여기서 HTTP POST 추가
        //  예시:
        //  val client = OkHttpClient()
        //  val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        //  val request = Request.Builder()
        //      .url("http://YOUR_NODE_SERVER/minwon")
        //      .post(body)
        //      .build()
        //  val response = client.newCall(request).execute()
        //  return response.isSuccessful

        true  // 지금은 항상 성공했다고 가정
    } catch (e: Exception) {
        Log.e("sendToNodeServer", "Node.js 전송 중 오류", e)
        false
    }
}

/**
 * JSON 문자열에 들어갈 값에서 큰따옴표, 줄바꿈 등을 이스케이프
 */
private fun escapeJson(value: String): String =
    value.replace("\"", "\\\"")
        .replace("\n", "\\n")
