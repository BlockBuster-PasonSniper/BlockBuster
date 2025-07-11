// 서버 실행에 필요한 핵심 함수들을 불러옴
import io.ktor.server.engine.*          // embeddedServer() 함수 제공
import io.ktor.server.netty.*           // Netty 엔진을 사용하기 위한 의존성
import io.ktor.server.application.*     // Ktor 애플리케이션 생명주기 및 확장 함수
import io.ktor.server.routing.*         // 라우팅 (URL 경로 지정)
import io.ktor.server.response.*        // 응답을 보내기 위한 call.respondText()
import routes.uploadRoutes

fun main() {
    // Ktor 서버를 Netty 엔진으로 생성 및 실행
    embeddedServer(
        Netty,                          // 사용할 서버 엔진 (Netty)
        port = 8080,                    // 서버 포트 설정
        host = "0.0.0.0"                // 외부 접속 허용 주소 (로컬호스트 외에도 접속 가능)
    ) {
        // 서버에 적용할 기능을 설정하는 블록 (예: 라우팅, 플러그인 설치 등)
        module()
    }.start(wait = true)                // 서버 시작 → 종료될 때까지 대기
}

// 서버의 주요 기능 설정 블록 (Ktor 2.x부터는 별도 함수로 빼는 것이 표준)
fun Application.module() {
    // URL 라우팅 설정
    routing {
        // GET 요청으로 '/'에 접근하면 실행되는 코드
        get("/") {
            // 문자열을 클라이언트로 응답 (예: 웹브라우저에서 출력됨)
            call.respondText("BLOCKBUSTER-ONLINE")
        }
        uploadRoutes()

    }
}