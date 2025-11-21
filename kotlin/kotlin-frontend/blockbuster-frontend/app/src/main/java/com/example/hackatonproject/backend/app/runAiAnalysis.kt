package com.example.hackatonproject.backend.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class AiResult(
    val category: String,
    val confidence: Float,
    val note: String = "AI Checked"
)

fun runAiAnalysis(context: Context, imagePath: String): AiResult {
//    val classNames = listOf("road damage", "illegal parking", "trash")
//    val imageSize = 224
//
//    // ✅ 모델 로컬 경로로 직접 로드
//    val modelFile = File(context.filesDir, "minwon_model.tflite") // 앱 내부 복사본 경로
//
//    // 📌 최초 실행 시 assets에서 지정된 위치로 복사
//    if (!modelFile.exists()) {
//        val originalModel = File(
//            context.getExternalFilesDir(null),
//            "minwon_model.tflite"
//        )
//        if (!originalModel.exists()) {
//            // 복사 경로가 정확한 외부 위치일 경우에만 복사 진행
////            val fallbackPath = "/data/user/0/${context.packageName}/files/minwon_model.tflite"
//            val fallbackPath = "minwon_model.tflite"
//            throw IOException("모델 파일이 존재하지 않음: $fallbackPath")
//        }
//        originalModel.copyTo(modelFile, overwrite = true)
//    }
//
//    val interpreter = Interpreter(loadFileToBuffer(modelFile))
//
//    // 이미지 로딩 및 전처리
//    val bitmap = BitmapFactory.decodeFile(imagePath)
//    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
//    val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
//
//    val output = Array(1) { FloatArray(classNames.size) }
//    interpreter.run(inputBuffer, output)
//
//    val predIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
//    val confidence = output[0][predIndex]
//    val category = classNames.getOrElse(predIndex) { "unknown" }
//
//    return AiResult(category = category, confidence = confidence)

    val classNames = listOf("road damage", "illegal parking", "trash")
    val imageSize = 224

    // 모델을 assets에서 직접 로딩
    val assetManager = context.assets
    val modelInputStream = assetManager.open("minwon_model.tflite")
    val modelBytes = modelInputStream.readBytes()
    val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
    modelBuffer.order(ByteOrder.nativeOrder())
    modelBuffer.put(modelBytes)
    modelBuffer.rewind()

    val interpreter = Interpreter(modelBuffer)

    // 이미지 로딩 및 전처리
    val bitmap = BitmapFactory.decodeFile(imagePath)
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
    val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

    val output = Array(1) { FloatArray(classNames.size) }
    interpreter.run(inputBuffer, output)

    val predIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
    val confidence = output[0][predIndex]
    val category = classNames.getOrElse(predIndex) { "unknown" }

    return AiResult(category = category, confidence = confidence)
}

private fun loadFileToBuffer(file: File): ByteBuffer {
    val inputStream = FileInputStream(file)
    val fileChannel = inputStream.channel
    val mapped = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    mapped.order(ByteOrder.nativeOrder())
    return mapped
}

private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val imageSize = 224
    val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
    byteBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(imageSize * imageSize)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (pixel in intValues) {
        val r = ((pixel shr 16) and 0xFF) / 255.0f
        val g = ((pixel shr 8) and 0xFF) / 255.0f
        val b = (pixel and 0xFF) / 255.0f
        byteBuffer.putFloat(r)
        byteBuffer.putFloat(g)
        byteBuffer.putFloat(b)
    }

    return byteBuffer
}
