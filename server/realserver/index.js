import express from "express";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import cors from "cors";
import os from "os";

// --- 기본 설정 ---
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = 3000;

// --- 미들웨어 ---
// CORS 설정 (모든 도메인에서 접근 허용)
app.use(cors());

// JSON 요청 본문을 파싱하기 위한 미들웨어 (필수! 외부 JSON을 받기 위함)
// 이미지 데이터 전송을 위해 limit를 50mb로 늘림
app.use(express.json({ limit: "50mb" }));

// 'public' 디렉토리의 정적 파일들(html, 최종 data.json)을 제공
app.use(express.static(path.join(__dirname, "public")));

// --- 경로 설정 ---
const PUBLIC_DIR = path.join(__dirname, "public");
const UPLOADS_DIR = path.join(PUBLIC_DIR, "uploads"); // 이미지 저장할 폴더
const OUTPUT_JSON_PATH = path.join(PUBLIC_DIR, "data.json");

// --- 서버 시작 시 초기 설정 (public 폴더 생성 등) ---
// public 폴더가 없으면 생성
if (!fs.existsSync(PUBLIC_DIR)) {
  fs.mkdirSync(PUBLIC_DIR, { recursive: true });
}
// public/uploads 폴더가 없으면 생성
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

console.log("💾 서버 시작 - 기존 데이터 유지 모드");

// 현재 PC의 IP 주소 가져오기
function getLocalIPAddress() {
  const interfaces = os.networkInterfaces();
  for (const interfaceName in interfaces) {
    const addresses = interfaces[interfaceName];
    for (const address of addresses) {
      // IPv4이고 내부 네트워크가 아닌 주소 찾기
      if (address.family === "IPv4" && !address.internal) {
        return address.address;
      }
    }
  }
  return "localhost"; // IP를 찾을 수 없으면 localhost 반환
}

const localIP = getLocalIPAddress();

// --- API 엔드포인트: 데이터 초기화 ---
app.delete("/api/reset-data", async (req, res) => {
  try {
    let deletedFiles = 0;
    let deletedImages = 0;

    // data.json 파일 삭제
    if (fs.existsSync(OUTPUT_JSON_PATH)) {
      fs.unlinkSync(OUTPUT_JSON_PATH);
      deletedFiles = 1;
      console.log("데이터 파일 삭제됨: data.json");
    }

    // uploads 폴더의 이미지들 삭제
    if (fs.existsSync(UPLOADS_DIR)) {
      const files = fs.readdirSync(UPLOADS_DIR);
      files.forEach((file) => {
        fs.unlinkSync(path.join(UPLOADS_DIR, file));
      });
      deletedImages = files.length;
      if (files.length > 0) {
        console.log(`이미지 파일 ${files.length}개 삭제됨`);
      }
    }

    res.status(200).json({
      success: true,
      message: "데이터 초기화 완료",
      deletedFiles,
      deletedImages,
    });

    console.log("🔄 데이터 초기화 완료 (웹에서 요청)");
  } catch (error) {
    console.error("데이터 초기화 중 오류:", error.message);
    res.status(500).json({
      success: false,
      message: `초기화 중 오류 발생: ${error.message}`,
    });
  }
});

// --- API 엔드포인트: 외부에서 JSON을 받아 처리 ---
app.post("/api/receive-json", async (req, res) => {
  console.log("외부로부터 데이터 수신:", req.body);

  let receivedData = req.body; // 외부에서 받은 JSON 데이터

  try {
    const { category, address, name, telno, pic } = receivedData;

    if (!category || !address || !name || !telno || !pic) {
      throw new Error(
        "모든 필드가 필요합니다: category, address, name, telno, pic"
      );
    }

    // 현재 시간 추가
    const timestamp = new Date().toISOString();

    // pic을 이미지 파일로 저장
    let imageUrl = null;
    if (pic.startsWith("data:image/")) {
      // Base64 데이터에서 'data:image/jpeg;base64,' 부분 제거
      const base64Image = pic.split(";base64,").pop();
      const imageBuffer = Buffer.from(base64Image, "base64");

      const imageFileName = `${name}_${Date.now()}.jpg`;
      const imagePathInUploads = path.join(UPLOADS_DIR, imageFileName);
      fs.writeFileSync(imagePathInUploads, imageBuffer);
      imageUrl = `/uploads/${imageFileName}`;

      console.log(`이미지 저장: ${imageFileName}`);
    }

    // 새로운 데이터 객체 생성
    const newRecord = {
      id: Date.now(),
      category,
      address,
      name,
      telno,
      pic: imageUrl || pic,
      timestamp,
    };

    // 기존 데이터 읽기 (없으면 빈 배열)
    let allData = [];
    if (fs.existsSync(OUTPUT_JSON_PATH)) {
      const existingData = fs.readFileSync(OUTPUT_JSON_PATH, "utf8");
      const parsed = JSON.parse(existingData);
      allData = Array.isArray(parsed) ? parsed : parsed.records || [];
    }

    // 새로운 레코드 추가
    allData.push(newRecord);

    // 전체 데이터를 JSON 파일로 저장
    const finalData = {
      totalCount: allData.length,
      lastUpdated: timestamp,
      records: allData,
    };

    fs.writeFileSync(
      OUTPUT_JSON_PATH,
      JSON.stringify(finalData, null, 2),
      "utf8"
    );

    console.log(`데이터 추가 완료! 총 ${allData.length}개 레코드`);

    res.status(200).json({
      success: true,
      message: `데이터 '${name}' 추가 완료`,
      recordId: newRecord.id,
      totalCount: allData.length,
      imageUrl: imageUrl,
    });
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
  console.log(`\n서버가 실행 중입니다:`);
  console.log(`  - 네트워크 접속: http://${localIP}:${port}`);
  console.log(`\nAPI 엔드포인트:`);
  console.log(`  - POST http://${localIP}:${port}/api/receive-json`);
  console.log(`\n웹 페이지:`);
  console.log(`  - 모바일: http://${localIP}:${port}/table.html`);
});
