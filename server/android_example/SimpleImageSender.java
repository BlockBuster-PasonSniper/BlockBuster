// SimpleImageSender.java
// Java 코드로 RealServer(포트 3000)에 5개 필드 데이터 전송

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class SimpleImageSender {
    
    // 현재 PC의 IP 주소를 동적으로 가져오는 함수
    private static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IP 주소 가져오기 실패: " + e.getMessage());
        }
        return "localhost"; // IP를 찾을 수 없으면 localhost 반환
    }
    
    // 동적으로 서버 URL 생성
    private static final String SERVER_URL = "http://" + getLocalIPAddress() + ":3000/api/receive-json";
    
    public String sendBusinessData(
            String category,
            String address,
            String name,
            String telno,
            String imageBase64
    ) {
        try {
            // JSON 데이터 생성
            String jsonData = String.format(
                "{\n" +
                "    \"category\": \"%s\",\n" +
                "    \"address\": \"%s\",\n" +
                "    \"name\": \"%s\",\n" +
                "    \"telno\": \"%s\",\n" +
                "    \"pic\": \"%s\"\n" +
                "}",
                category, address, name, telno, imageBase64
            );
            
            // HTTP 연결 설정
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // 데이터 전송
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonData.getBytes());
                outputStream.flush();
            }
            
            // 응답 받기
            int responseCode = connection.getResponseCode();
            String response;
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    response = sb.toString();
                }
            } else {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    response = sb.toString();
                }
            }
            
            connection.disconnect();
            
            return "Response Code: " + responseCode + "\nResponse: " + response;
            
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 파일을 Base64로 변환하는 함수
    public String fileToBase64(String filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            System.err.println("파일 읽기 실패: " + e.getMessage());
            return "";
        }
    }
    
    // 사용 예제
    public static void main(String[] args) {
        SimpleImageSender sender = new SimpleImageSender();
        
        System.out.println("서버 URL: " + SERVER_URL);
        
        // 실제 이미지 파일을 사용한 테스트
        String base64Image = sender.fileToBase64("simyoung.jpg");
        if (!base64Image.isEmpty()) {
            String result = sender.sendBusinessData(
                "음식점",
                "서울시 강남구 테헤란로 123",
                "심영이네 맛집",
                "02-1234-5678",
                base64Image
            );
            System.out.println("전송 결과:");
            System.out.println(result);
        } else {
            System.out.println("이미지 파일을 찾을 수 없습니다.");
        }
    }
}
