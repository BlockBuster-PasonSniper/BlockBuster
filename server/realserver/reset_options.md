// 추가 옵션: 이미지만 초기화하고 싶은 경우
// index.js의 초기화 부분을 이렇게 수정하세요:

const shouldResetData = process.env.RESET_DATA !== 'false';
const shouldResetImages = process.env.RESET_IMAGES !== 'false';

if (shouldResetData && fs.existsSync(OUTPUT_JSON_PATH)) {
fs.unlinkSync(OUTPUT_JSON_PATH);
console.log("이전 데이터 파일 삭제됨: data.json");
}

if (shouldResetImages && fs.existsSync(UPLOADS_DIR)) {
const files = fs.readdirSync(UPLOADS_DIR);
files.forEach(file => {
fs.unlinkSync(path.join(UPLOADS_DIR, file));
});
if (files.length > 0) {
console.log(`이전 이미지 파일 ${files.length}개 삭제됨`);
}
}

// 사용 예:
// RESET_DATA=false RESET_IMAGES=true node index.js (데이터는 유지, 이미지만 삭제)
// RESET_DATA=true RESET_IMAGES=false node index.js (데이터만 삭제, 이미지는 유지)
