## MSA 기반 택시 호출 Backend 프로젝트

---

- common : 공통 모듈 (예외처리, JWT, 응답처리 등)
- config-server : Spring Cloud Config를 이용한 설정 관리
- eureka-server : 서비스 디스커버리 (Eureka)
- gateway : API Gateway (Spring Cloud Gateway)
- notification-service : 알림 서비스 (kafka 기반) - 예정
- ride-service : 택시 호출 및 운행 관리
- user-service : 회원 서비스 (회원가입, 로그인 등)