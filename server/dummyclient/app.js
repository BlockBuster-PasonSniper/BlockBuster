import express from "express";
import fs from "fs/promises"; // For async file operations
import path from "path";
import { fileURLToPath } from "url";
import fetch from "node-fetch"; // For making HTTP requests

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = 4000; // Dummy Client will run on port 4000

// Serve static files from the current directory (where index.html is)
app.use(express.static(__dirname));

// Endpoint to send image to realserver
// Now accepts imageName as a query parameter
app.get("/send-image-to-realserver", async (req, res) => {
  const imageName = req.query.imageName; // Get image name from query parameter
  if (!imageName) {
    return res.status(400).json({
      success: false,
      message: "imageName 쿼리 파라미터가 필요합니다.",
    });
  }

  const imagePath = path.join(__dirname, imageName);

  try {
    // Check if file exists
    await fs.access(imagePath);

    // Read image file
    const imageBuffer = await fs.readFile(imagePath);
    const base64Image = `data:image/jpeg;base64,${imageBuffer.toString(
      "base64"
    )}`;

    let title = "";
    let description = "";
    let imageDescription = "";

    if (imageName === "simyoung.jpg") {
      title = "심영 이미지 전송 (Node.js Client)";
      description = "Node.js 더미 클라이언트에서 보낸 심영 이미지입니다.";
      imageDescription = "Node.js 클라이언트가 보낸 심영의 모습";
    } else if (imageName === "yoon.jpg") {
      title = "윤 이미지 전송 (Node.js Client)";
      description = "Node.js 더미 클라이언트에서 보낸 윤 이미지입니다.";
      imageDescription = "Node.js 클라이언트가 보낸 윤의 모습";
    } else {
      title = `${imageName} 전송 (Node.js Client)`;
      description = `Node.js 더미 클라이언트에서 보낸 ${imageName} 이미지입니다.`;
      imageDescription = `${imageName} 이미지`;
    }

    const payload = {
      title: title,
      description: description,
      imageFileName: imageName,
      imageDescription: imageDescription,
      imageData: base64Image,
    };

    // Send to realserver
    const realserverResponse = await fetch(
      "http://localhost:3000/api/receive-json",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      }
    );

    const result = await realserverResponse.json();

    if (realserverResponse.ok) {
      res.status(200).json({
        success: true,
        message: result.message, // Real Server의 메시지 직접 전달
        realserverResponse: result,
      });
    } else {
      res.status(realserverResponse.status).json({
        success: false,
        message: "이미지 전송 실패",
        realserverResponse: result,
      });
    }
  } catch (error) {
    if (error.code === "ENOENT") {
      res.status(404).json({
        success: false,
        message: `파일을 찾을 수 없습니다: ${imageName}`,
        error: error.message,
      });
    } else {
      console.error("Error sending image:", error);
      res.status(500).json({
        success: false,
        message: "서버 오류 발생",
        error: error.message,
      });
    }
  }
});

app.listen(port, () => {
  console.log(
    `Dummy Client 서버가 http://localhost:${port} 에서 실행 중입니다.`
  );
  console.log(
    `이미지 전송을 위해 http://localhost:${port}/index.html 에 접속하여 버튼을 클릭하세요.`
  );
});
