import express from "express";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

// --- 기본 설정 ---
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = 3000;

// --- 미들웨어 ---
// JSON 요청 본문을 파싱하기 위한 미들웨어 (필수! 외부 JSON을 받기 위함)
app.use(express.json({ limit: "20mb" }));

// 'public' 디렉토리의 정적 파일들(html, 최종 data.json)을 제공
app.use(express.static(path.join(__dirname, "public")));

// --- 경로 설정 ---
const DUMMY_DIR = path.join(__dirname, "dummy");
const PUBLIC_DIR = path.join(__dirname, "public");
const OUTPUT_JSON_PATH = path.join(PUBLIC_DIR, "data.json");

// --- 서버 시작 시 초기 설정 (public 폴더 생성 등) ---
// public 폴더가 없으면 생성
if (!fs.existsSync(PUBLIC_DIR)) {
  fs.mkdirSync(PUBLIC_DIR, { recursive: true });
}

// --- API 엔드포인트: 외부에서 JSON을 받아 처리 ---
app.post("/api/receive-json", async (req, res) => {
  console.log("외부로부터 JSON 데이터 수신:", req.body);

  let receivedData = req.body; // 외부에서 받은 JSON 데이터

  try {
    // 1. dummy 폴더를 스캔하여 .jpg 파일을 동적으로 찾기
    const allFiles = fs.readdirSync(DUMMY_DIR);
    const jpgFile = allFiles.find((file) =>
      file.toLowerCase().endsWith(".jpg")
    );

    if (!jpgFile) {
      throw new Error("dummy 폴더에서 JPG 파일을 찾을 수 없습니다.");
    }
    const DUMMY_IMAGE_PATH = path.join(DUMMY_DIR, jpgFile);

    // 2. 찾은 이미지 파일을 읽어서 Base64 데이터 URL로 변환
    const imageBuffer = fs.readFileSync(DUMMY_IMAGE_PATH);
    const base64Image = `data:image/jpeg;base64,${imageBuffer.toString(
      "base64"
    )}`;
    console.log(`'${jpgFile}' 이미지를 Base64로 변환했습니다.`);

    // 3. 받은 JSON 데이터에 이미지 정보를 추가
    receivedData.imageData = base64Image;
    receivedData.imageFileName = jpgFile; // 어떤 파일이 변환되었는지 이름도 추가
    receivedData.imageDescription =
      receivedData.imageDescription || `${path.parse(jpgFile).name} 이미지`; // 외부 JSON에 설명이 없으면 파일 이름으로 설정

    // 4. 최종 결과물을 public/data.json 파일로 저장
    fs.writeFileSync(
      OUTPUT_JSON_PATH,
      JSON.stringify(receivedData, null, 2),
      "utf8"
    );
    console.log(
      `성공! 이미지 데이터가 포함된 최종 JSON 파일을 생성했습니다: ${OUTPUT_JSON_PATH}`
    );

    res
      .status(200)
      .json({ success: true, message: "데이터가 성공적으로 처리되었습니다." });
  } catch (error) {
    console.error("데이터 처리 중 오류 발생:", error.message);
    res.status(500).json({
      success: false,
      message: `데이터 처리 중 오류 발생: ${error.message}`,
    });
  }
});

// --- 서버 실행 ---
app.listen(port, () => {
  console.log(`\n서버가 http://localhost:${port} 에서 실행 중입니다.`);
  console.log(
    `JSON을 보내려면 POST 요청을 http://localhost:${port}/api/receive-json 으로 보내세요.`
  );
  console.log(
    `웹 페이지는 http://localhost:${port}/index.html 에서 확인하세요.`
  );
});
