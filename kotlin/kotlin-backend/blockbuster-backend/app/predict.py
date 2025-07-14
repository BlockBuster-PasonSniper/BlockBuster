import sys
import json
import numpy as np
from PIL import Image
import tensorflow as tf

# 클래스 이름 정의
class_names = [
  "road damage",
  "illegal parking",
  "trash"
]

# 예외처리 추가: 모델 로딩 및 예측 과정에서 문제 발생 시 JSON으로 반환
try:
    # 모델 로드
    interpreter = tf.lite.Interpreter(model_path="minwon_model.tflite")
    interpreter.allocate_tensors()

    # 텐서 정보 추출
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # 이미지 경로 인자로 받기
    image_path = sys.argv[1]

    # 이미지 전처리 함수
    def preprocess_image(image_path):
        img = Image.open(image_path).convert("RGB")
        img = img.resize((224, 224))  # 모델에 맞는 크기로 조정
        img_array = np.array(img, dtype=np.float32) / 255.0
        return np.expand_dims(img_array, axis=0)

    input_data = preprocess_image(image_path)

    # 모델 입력 설정 및 추론 실행
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    # 결과 해석
    pred_index = int(np.argmax(output_data))
    confidence = float(np.max(output_data))

    # 응답 형식 구성
    result = {
        "category": class_names[pred_index],
        "confidence": confidence,
        "note": "AI Checked"
    }

    # JSON 형식으로 결과 출력 (Kotlin 백엔드에서 읽음)
    print(json.dumps(result, ensure_ascii=False))

except Exception as e:
    # 오류 발생 시 JSON 형식으로 에러 내용 출력
    error_result = {
        "error": "예측 중 오류 발생",
        "message": str(e)
    }
    print(json.dumps(error_result, ensure_ascii=False))

with open("ai_result.json", "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)
