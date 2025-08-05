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

try:
    # 모델 로드
    interpreter = tf.lite.Interpreter(model_path="minwon_model.tflite")
    interpreter.allocate_tensors()

    # 입력 및 출력 텐서 정보
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # 이미지 경로 및 출력 경로 인자
    image_path = sys.argv[1]
    output_json_path = sys.argv[2]

    # 이미지 전처리 함수
    def preprocess_image(image_path):
        img = Image.open(image_path).convert("RGB")
        img = img.resize((224, 224))  # 모델 입력 크기에 맞춤
        img_array = np.array(img, dtype=np.float32) / 255.0
        return np.expand_dims(img_array, axis=0)

    input_data = preprocess_image(image_path)

    # 모델 입력 설정 및 추론 실행
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    # 결과 처리
    pred_index = int(np.argmax(output_data))
    confidence = float(np.max(output_data))

    result = {
        "category": class_names[pred_index],
        "confidence": confidence,
        "note": "AI Checked"
    }

    # 콘솔에 출력 (stdout으로 보내기, 백엔드는 파일로만 사용 중)
    print(json.dumps(result, ensure_ascii=False))

    # 결과를 JSON 파일로 저장
    with open(output_json_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

except Exception as e:
    error_result = {
        "error": "예측 중 오류 발생",
        "message": str(e)
    }
    print(json.dumps(error_result, ensure_ascii=False))
    with open("ai_result.json", "w", encoding="utf-8") as f:
        json.dump(error_result, f, ensure_ascii=False, indent=2)
