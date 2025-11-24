package com.example.hackatonproject.backend.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AiResult(
    val category: String,
    val confidence: Float,
    val detailCode: String,
    val note: String = "AI Checked"
)

fun runAiAnalysis(context: Context, imagePath: String): AiResult {

    val classNames = listOf(
        "D00", "D10", "D20", "D30", "D40",
        "D50", "D60", "D70", "D80", "D90"
    )

    val CONFIDENCE_THRESHOLD = 0.7f

    // 1) 모델 로드
    val assetManager = context.assets
    val modelBytes = assetManager.open("minwon_model.tflite").use { it.readBytes() }
    val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
        order(ByteOrder.nativeOrder())
        put(modelBytes)
        rewind()
    }

    val interpreter = Interpreter(modelBuffer)

    // === 입력/출력 텐서 정보 로그 ===
    val inputTensor = interpreter.getInputTensor(0)
    val inputShape = inputTensor.shape()
    val inputType = inputTensor.dataType()
    val inputQ = inputTensor.quantizationParams()

    Log.d("AI_DEBUG", "---- TFLite INPUT TENSOR ----")
    Log.d("AI_DEBUG", "shape = [${inputShape.joinToString()}]")
    Log.d("AI_DEBUG", "dataType = $inputType")
    Log.d("AI_DEBUG", "quant scale = ${inputQ.scale}, zeroPoint = ${inputQ.zeroPoint}")

    val outputTensor = interpreter.getOutputTensor(0)
    val outputShape = outputTensor.shape()
    Log.d("AI_DEBUG", "---- TFLite OUTPUT TENSOR ----")
    Log.d("AI_DEBUG", "shape = [${outputShape.joinToString()}]")

    val (inputHeight, inputWidth, inputChannels) = when (inputShape.size) {
        4 -> Triple(inputShape[1], inputShape[2], inputShape[3])
        3 -> Triple(inputShape[0], inputShape[1], inputShape[2])
        else -> throw IllegalStateException(
            "예상치 못한 입력 텐서 shape: [${inputShape.joinToString()}]"
        )
    }

    Log.d(
        "AI_DEBUG",
        "모델이 기대하는 입력: ${inputWidth}x$inputHeight, channels=$inputChannels"
    )

    // 2) 이미지 로딩 + 전처리
    val bitmap = BitmapFactory.decodeFile(imagePath)
        ?: throw IOException("이미지 파일을 읽을 수 없습니다: $imagePath")

    val resizedBitmap = Bitmap.createScaledBitmap(
        bitmap,
        inputWidth,
        inputHeight,
        true
    )

    // === 여기서부터 전처리 방식이 핵심 ===
    val inputBuffer = if (inputType == DataType.FLOAT32) {
        // 학습이 float 모델 기준이면 이쪽이 맞음
        convertBitmapToFloatBuffer(resizedBitmap, inputWidth, inputHeight, inputChannels)
    } else {
        // 만약 tflite가 UINT8/INT8 양자화라면, 실제론 이쪽으로 맞춰줘야 함
        convertBitmapToQuantizedBuffer(
            resizedBitmap,
            inputWidth,
            inputHeight,
            inputChannels,
            inputQ.scale,
            inputQ.zeroPoint
        )
    }

    // 입력 샘플 몇 개만 찍어보기 (값이 다 0.0 비슷하면 전처리 문제)
    val sample = FloatArray(20)
    if (inputType == DataType.FLOAT32) {
        val dup = inputBuffer.duplicate()
        dup.order(ByteOrder.nativeOrder())
        dup.rewind()
        for (i in sample.indices) {
            if (dup.remaining() >= 4) sample[i] = dup.float else break
        }
        Log.d("AI_DEBUG", "input sample floats = ${sample.joinToString()}")
    }

    // 3) 추론 실행
    val output = Array(1) { FloatArray(classNames.size) }
    interpreter.run(inputBuffer, output)

    val scores = output[0]

    // 모든 클래스 score 로그
    Log.d("AI_DEBUG", "---- CLASS SCORES ----")
    scores.forEachIndexed { index, score ->
        val name = classNames.getOrElse(index) { "UNK$index" }
        Log.d("AI_DEBUG", "score[$index] ($name) = $score")
    }

    val predIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

    if (predIndex == -1) {
        Log.e("AI_DEBUG", "모델 예측 인덱스 계산 실패 (predIndex = -1)")
        return AiResult("기타", 0f, "ETC", "PREDICTION_INDEX_ERROR")
    }

    val confidence = scores[predIndex]
    val detailCodeRaw = classNames.getOrElse(predIndex) { "UNKNOWN" }

    // 컨피던스 threshold 이하 → 기타
    if (confidence < CONFIDENCE_THRESHOLD) {
        Log.w(
            "AI_DEBUG",
            "Low confidence result: $confidence < $CONFIDENCE_THRESHOLD, 기타로 처리"
        )
        return AiResult(
            category = "기타",
            confidence = confidence,
            detailCode = "ETC",
            note = "LOW_CONFIDENCE"
        )
    }

    val category = when (detailCodeRaw) {
        "D00", "D10", "D20" -> "도로 균열"
        "D40"               -> "포트홀"
        "D30", "D80"        -> "보수 미흡(패치/보수)"
        "D90"               -> "포장 변형"
        "D50", "D60"        -> "표지시설 마모(횡단보도/차선)"
        "D70"               -> "배수시설 문제(맨홀)"
        else                -> "기타 도로 이상"
    }

    Log.d(
        "AI_DEBUG",
        "예측 결과: detailCode=$detailCodeRaw, category=$category, confidence=$confidence"
    )

    return AiResult(
        category = category,
        confidence = confidence,
        detailCode = detailCodeRaw
    )
}

private fun convertBitmapToFloatBuffer(
    bitmap: Bitmap,
    width: Int,
    height: Int,
    channels: Int
): ByteBuffer {
    val bytesPerChannel = 4
    val buffer = ByteBuffer.allocateDirect(bytesPerChannel * width * height * channels)
    buffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(width * height)
    bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

    for (pixel in intValues) {
        // 🔹 정규화 제거: 0~255 그대로 float로 넣어줌
        val r = ((pixel shr 16) and 0xFF).toFloat()
        val g = ((pixel shr 8) and 0xFF).toFloat()
        val b = (pixel and 0xFF).toFloat()

        buffer.putFloat(r)
        buffer.putFloat(g)
        buffer.putFloat(b)
    }

    buffer.rewind()
    return buffer
}


private fun convertBitmapToQuantizedBuffer(
    bitmap: Bitmap,
    width: Int,
    height: Int,
    channels: Int,
    scale: Float,
    zeroPoint: Int
): ByteBuffer {
    // UINT8 / INT8용 예시. 지금은 주로 UINT8 가정.
    val buffer = ByteBuffer.allocateDirect(width * height * channels)
    buffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(width * height)
    bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

    for (pixel in intValues) {
        val r = ((pixel shr 16) and 0xFF)
        val g = ((pixel shr 8) and 0xFF)
        val b = (pixel and 0xFF)

        // 가장 단순한: 0~255 그대로 넣기
        // 필요하면 scale/zeroPoint 반영해서 다시 조정 가능
        buffer.put(r.toByte())
        buffer.put(g.toByte())
        buffer.put(b.toByte())
    }

    buffer.rewind()
    return buffer
}
