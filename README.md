# MSA 기반 택시 호출 Backend 프로젝트
## 기술 스택
- Backend : java 17, Spring Boot 3.3.2
- MSA : Spring Cloud (Eureka, Config, Gateway)
- Database : MySQL, Redis
- Messaging : Kafka (알림 서비스 예정)
- Security : Spring Security, OAuth2, JWT

---

## 프로젝트 흐름
1. **회원 가입**
   - OAuth2, 자체 회원 가입 (승객, 기사)

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
   - 승객과 기사에게 필요 내용 알림 (kafka, 'notification-service' 예정)

---

## 모듈별 역할
- common : 공통 모듈 (예외처리, JWT, 응답처리 등)
- config-server : Spring Cloud Config를 이용한 설정 관리
- eureka-server : 서비스 디스커버리 (Eureka)
- gateway : API Gateway (Spring Cloud Gateway), JWT 인증 필터 포함
- notification-service : 알림 서비스 (kafka 기반) - 예정
- ride-service : 택시 호출 및 운행 관리 서비스
- user-service : 회원 서비스 (회원가입, 로그인 등)

---