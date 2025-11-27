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

    // ✅ TFLite 모델 출력: 8클래스 (D70까지 존재)
    //   하지만 코드에서는 D70을 순위/출력에서 무시
    val classNames = listOf(
        "D00", "D10", "D20", "D30",
        "D40", "D50", "D60", "D70"
    )

    val CONFIDENCE_THRESHOLD = 0.7f
    val EXCLUDE_D20_THRESHOLD = 0.75f

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

    // (선택) 안전장치: 모델 출력 차원과 classNames.size가 다른 경우 로그 남기기
    if (outputShape.size == 2 && outputShape[1] != classNames.size) {
        Log.e(
            "AI_DEBUG",
            "모델 출력 클래스 수(${outputShape[1]})와 classNames.size(${classNames.size})가 다릅니다!"
        )
    }

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

    val inputBuffer = if (inputType == DataType.FLOAT32) {
        convertBitmapToFloatBuffer(resizedBitmap, inputWidth, inputHeight, inputChannels)
    } else {
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
    if (inputType == DataType.FLOAT32) {
        val sample = FloatArray(20)
        val dup = inputBuffer.duplicate()
        dup.order(ByteOrder.nativeOrder())
        dup.rewind()
        for (i in sample.indices) {
            if (dup.remaining() >= 4) sample[i] = dup.float else break
        }
        Log.d("AI_DEBUG", "input sample floats = ${sample.joinToString()}")
    }

    // 3) 추론 실행
    val output = Array(1) { FloatArray(classNames.size) }   // 8개 출력
    interpreter.run(inputBuffer, output)

    val scores = output[0]

    // 모든 클래스 score 로그 (D70 포함해서 그대로 출력은 함)
    Log.d("AI_DEBUG", "---- CLASS SCORES ----")
    scores.forEachIndexed { index, score ->
        val name = classNames.getOrElse(index) { "UNK$index" }
        Log.d("AI_DEBUG", "score[$index] ($name) = $score")
    }

    // === 4) D20 제외 조건 + D70 무시 로직 ===

    // 항상 D70은 순위 경쟁에서 제외
    val indexD70 = classNames.indexOf("D70")
    val candidateIndicesBase = scores.indices.filter { it != indexD70 }

    // D20 인덱스
    val indexD20 = classNames.indexOf("D20")

    // D20까지 제외한 후보 (D20 규칙 발동시 사용)
    val nonD20Indices = candidateIndicesBase.filter { it != indexD20 }

    // strong 클래스: D00, D10, D30, D40, D50, D60 (D70은 여기서도 제외)
    val strongClasses = setOf("D00", "D10", "D30", "D40", "D50", "D60")
    val strongIndices = strongClasses.mapNotNull { className ->
        classNames.indexOf(className).takeIf { it >= 0 }
    }

    // strong 클래스들 중 하나라도 EXCLUDE_D20_THRESHOLD 이상이면 → D20은 무조건 배제
    val hasStrongHighConfidence = strongIndices.any { idx ->
        scores[idx] >= EXCLUDE_D20_THRESHOLD
    }

    val predIndex = if (hasStrongHighConfidence && indexD20 >= 0) {
        // 👉 규칙 발동: D20 + D70을 빼고 나머지 중에서 가장 높은 score 선택
        val idx = nonD20Indices.maxByOrNull { scores[it] } ?: -1
        Log.w(
            "AI_DEBUG",
            "D20_EXCLUDED_RULE_TRIGGERED: strong class >= $EXCLUDE_D20_THRESHOLD, " +
                    "selected=${classNames.getOrElse(idx) { "UNK" }} score=${scores.getOrNull(idx)}"
        )
        idx
    } else {
        // 평소처럼 D70만 제외한 상태에서 가장 높은 score 선택
        val idx = candidateIndicesBase.maxByOrNull { scores[it] } ?: -1
        idx
    }

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

    //D00 종방향균열
    //D10 횡방향균열
    //D20 악어등균열
    //D30 보수된균열
    //D40 포트홀
    //D50 횡단보도 흐림
    //D60 차선마모
    //D70 맨홀뚜껑 (⚠ 코드상에선 순위/출력에서 이미 제외됨)
    val category = when (detailCodeRaw) {
        "D00", "D10", "D20" -> "도로 균열"
        "D40"               -> "포트홀"
        "D30"               -> "보수 미흡(패치/보수)"
        "D50", "D60"        -> "표지시설 마모(횡단보도/차선)"
        "D70"               -> "배수시설 문제(맨홀)" // 이 분기는 사실상 도달하지 않음
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
    val buffer = ByteBuffer.allocateDirect(width * height * channels)
    buffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(width * height)
    bitmap.getPixels(intValues, 0, width, 0, 0, width, height)

    for (pixel in intValues) {
        val r = ((pixel shr 16) and 0xFF)
        val g = ((pixel shr 8) and 0xFF)
        val b = (pixel and 0xFF)

        buffer.put(r.toByte())
        buffer.put(g.toByte())
        buffer.put(b.toByte())
    }

    buffer.rewind()
    return buffer
}
