# 택시 호출 서비스 (MSA 구조 학습용 프로젝트)
본 프로젝트는 모놀리식이 아닌 MSA 구조를 직접 설계 및 구현해보고자 시작한 프로젝트입니다.

서비스 분리 및 서비스 간 통신 등을 직접 적용하며,
MSA 구조가 갖는 장점과 복잡도를 모두 경험하는 것을 목표로 했습니다.

---

## 기술 스택
- Backend : java 17, Spring Boot 3.3.2
- MSA : Spring Cloud (Eureka, Config, Gateway)
- Database : MySQL, Redis
- Messaging : Kafka(서비스 간 이벤트 전달), WebSocket STOMP
- Security : Spring Security, JWT, OAuth2(소셜 로그인 구성만 완료, 추후 테스트 예정)


## 프로젝트 흐름
1. **회원 가입**
   - OAuth2(Google, Kakao, Naver 구성만 완료), 자체 회원 가입 (승객, 기사)

2. **기사 추가 정보 등록**
   - 차량 정보, 면허 등

3. **승객이 택시 호출**
   - 출발지, 목적지 입력 후 호출
   - 'ride-service'에서 호출 정보 저장 (Redis)

4. **기사가 호출 조회**
   - 기사 위치를 기반으로 최대 5km 이내 호출들 조회(Redis Geo)

5. **호출 수락 및 운행정보**
   - 호출 수락 시 운행에 대한 정보 저장 (MySQL)
   - 호출 수락, 취소, 완료 처리
   - kafka를 통해 메시지 전송(ride-service) -> notification-service에서 이벤트 수신
   - notification-service에서 WebSocket(STOMP)로 실시간 메시지 전송


## 모듈별 역할
- common : 공통 모듈 (예외처리, JWT, 응답처리 등)
- config-server : Spring Cloud Config를 이용한 설정 관리
- eureka-server : 서비스 디스커버리 (Eureka)
- gateway : API Gateway (Spring Cloud Gateway), JWT 인증 필터 포함
- notification-service : Kafka 기반 이벤트 처리 및 WebSocket(STOMP)로 실시간 메시지 전송
- ride-service : 택시 호출 및 운행 관리 서비스
- user-service : 회원 서비스 (회원가입, 로그인 등)


## 주요 API
### 회원가입 및 인증
| 메서드   | 경로 | 설명      |
|-------|------|---------|
| `POST` | `/api/user/register` | 일반 회원가입 |
| `POST` | `/api/auth/login` | 일반 로그인  |
| `POST` | `/api/auth/logout` | 로그아웃    |
| `POST` | `/api/auth/refresh` | 토큰 재발급 |

### 기사 정보
| 메서드     | 경로                     | 설명       |
|---------|------------------------|----------|
| `POST`  | `/api/driver/register` | 기사 정보 등록 |
| `PATCH` | `/api/driver/info`     | 기사 정보 수정 |
| `PATCH` | `/api/driver/status`   | 상태 변경    |

### 호출 및 운행
| 메서드   | 경로                            | 설명           |
|-------|-------------------------------|--------------|
| `POST` | `/api/ride/call`              | 택시 호출        |
| `POST` | `/api/ride/find`              | 호출 목록 조회(기사) |
| `POST` | `/api/ride/accept`            | 호출 수락        |
| `POST` | `/api/ride/cancel/{rideId}` | 호출 취소        |
| `POST` | `/api/ride/start/{rideId}` | 운행 시작        |
| `POST` | `/api/ride/complete` | 운행 종료        |
