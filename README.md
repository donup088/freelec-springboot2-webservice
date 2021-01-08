## 스프링 부트와 AWS로 혼자 구현하는 웹 서비스

---

### 2장

- jsonPath 사용
    - JSON 응답값을 필드별로 검증할 수 있는 메소드
    - $를 기준으로 필드명 명시
    ```
     mvc.perform(get("/hello/dto")
                    .param("name", name)
                    .param("amount", String.valueOf(amount)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name",is(name)))
                    .andExpect(jsonPath("$.amount",is(amount)));
    ```

---

### 3장

- 패키지 구조
    - 프로젝트의 규모가 커질 경울 domain과 해당하는 repository는 함께 관리하도록 한다.
- 도메인 설계
    - Entity 클래스에서는 Setter 메소드를 만들지 않는다. 필요할 경우 목적과 의도를 나타낼 수 있는 메소드를 추가한다.
- Dto 만들기
    - Entity 클래스와 controller에서 쓸 Dto는 항상 분리해야한다.(Dto는 View를 위한 클래스라 변경이 자주 일어난다.)
    - Requestdto와 ResponseDto를 구분하여 사용한다.
- api 조회시 return은 entity가 아닌 Dto로 하기
    - repository에서 조회해오는 것을 entity로 하면 Dto에서 entity를 매개변수로 받는 생성자를 통해 entity를 Dto로 바꾼 뒤 반환
- JPA Auditing 으로 생성시간/수정시간 자동화하기
    1.
    ```
      @Getter
      @MappedSuperclass    //BaseEntity를 상속할 경우 필드들도 컬럼으로 인식하도록 한다.
      @EntityListeners(AuditingEntityListener.class)    //클래스에 Aud
      public abstract class BaseTimeEntity {
          @CreatedDate
          private LocalDateTime createdDate;
      
          @LastModifiedDate
          private LocalDateTime modifiedDate;
      }
    ```
    2.
     ```
     BaseTimeEntity를 사용할 entity에서 상속받기
     ```
    3.
    ```
      @EnableJpaAuditing //JPA Auditing 기능 활성화
      @SpringBootApplication
      public class Springboot2WebserviceApplication {
        public static void main(String[] args) {
        SpringApplication.run(Springboot2WebserviceApplication.class, args);
        }
      }
    ```

---  

### 4장

- css 파일과 js 파일 추가 위치
    - css파일은 header 에 추가해주고 js 파일은 footer에 추가해준다.
    - html은 코드가 위부터 실행되기 때문에 head가 다 실행되고 body가 실행된다. js를 hear에 두면 js의 용량이 클수록 body 부분의 실행이 늦어진다.
    - css는 head에서 불러오지 않으면 css가 적용되지 않은 깨진 화면을 사용자가 볼 수 있다.
- javascript 파일 관리
    - resources/static에 따로 파일을 만든다. ex) /js/app/index.js
    - 따로 파일을 만든 뒤 footer에 추가해준다.
    ```
    <script src="/js/app/index.js"></script>
    ```
- Entity 를 Dto로 바꿔서 반환하기
    - repository에서 entity로 조회를 했다면 service에서 stream의 map,collect 등을 사용하여 Dto로 변환시키고 반환할 수 있다.
    ```
       return postsRepository.findAllDesc().stream()
                .map(PostsListResponseDto::new)
                .collect(Collectors.toList());
    ```

---

### 5장

#### 구글 로그인 연동

- 구글 서비스 등록
    - 구글 클라우드 플랫폼으로 가서 새 프로젝트 생성
    - 왼쪽 메뉴탭을 클릭하여 API 및 서비스 카테고리로 이동 후 사용자 인증 정보 만들기
    - OAuth 클라이언트 ID 만들기, 동의 화면 입력, 하단에 승인된 리디렉션 URI http://localhost:8080/login/oauth2/code/google로 설정 (스프링 부트 2 버전의
      시큐리티에서는 기본적으로 다음과 같이 설정함)
    - 생성된 클라이언트 목록에 가서 클라이언트 ID와 클라이언트 보안비밀을 프로젝트에서 설정
- application.yml 파일에 추가
  ```
  spring:
    security:
      oauth2:
        client:
          registration:
            google:
              client-id: 클라이언트 아이디
              client-secret: 클라이언트 보안비밀
              scope:
                - email
                - profile
  ```
- compile('org.springframework.boot:spring-boot-starter-oauth2-client') build.gradle에 추가
- CustomOAuth2UserService 구현
  ```
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest,OAuth2User> auth2UserService = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = auth2UserService.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); 
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.
                of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes);
        httpSession.setAttribute("user", new SessionUser(user));

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(),attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
  ```
    - registrationId 는 현재 로그인 진행 중인 서비스를 구분하는 코드 ex) 구글인지 네이버인지
    - userNameAttributeName 은 OAuth2 로그인 진행 시 키가 되는 필드 값 (구글의 경우 기본적으로 코드 지원 "sub")
    - OAuthAttributes OAuth2User의 attribute를 담을 클래스
    - SessionUser 세션에 사용자 정보를 저장하기 위한 Dto 클래스
    - 구글 사용자 정보가 업데이트 되었을 때 대비하여 update 구현
- OAuthAttributes 클래스
  ```
      @Getter
      public class OAuthAttributes {
      private Map<String, Object> attributes;
      private String nameAttributeKey;
      private String name;
      private String email;
      private String picture;
          
       ...
  
      public static OAuthAttributes of(String registrationId,
                                       String userNameAttributeName,
                                       Map<String, Object> attributes) {
          return ofGoogle(userNameAttributeName, attributes);
      }
  
      private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                              Map<String, Object> attributes) {
          return OAuthAttributes.builder()
                  .name((String) attributes.get("name"))
                  .email((String) attributes.get("email"))
                  .picture((String) attributes.get("picture"))
                  .attributes(attributes)
                  .nameAttributeKey(userNameAttributeName)
                  .build();
      }
      ...
    }
  ```
- SessionUser 클래스
  ```
    @Getter
  public class SessionUser implements Serializable {
  private String name;
  private String email;
  private String picture;

    public SessionUser(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.picture = user.getPicture();
    }
  }
  ```
  - Serializable(직렬화) 사용
  - 자바 직렬화란 자바 시스템 내부에서 사용되는 객체 또는 데이터를 외부의 자바 시스템에서도 사용할 수 있도록 
    바이트(byte) 형태로 데이터 변환하는 기술이다.
- SecurityConfig에 CustomOAuth2UserService 사용하도록 설정
  ```
     http.oauth2Login()
                  .userInfoEndpoint()
                  .userService(customOAuth2UserService)
  ```
#### 네이버 로그인 연동
- 네이버 API 등록
  - 네이버 오픈 API로 이동 후 네이버 서비스 등록
  - 서비스 URL http://localhost:8080/ 
  - Callback URL : http://localhost:8080/login/oauth2/code/naver
  - 네이버 서비스 등록 완료 후 클라이언트 아이디와 클라이언트 Secret을 프로젝트에 설정
- 프로젝트 설정
  application.yml 에 추가
  ```
  spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            client-id: 클라이언트 아이디
            client-secret: 클라이언트 Secret
            scope:
              - name
              - email
              - profile_image
            redirect_uri: "{baseUrl}/{action}/oauth2/code/{registrationId}"
            authorization_grant_type: authorization_code
            client-name: Naver


        provider:
          naver:
            authorization_uri: https://nid.naver.com/oauth2.0/authorize
            token_uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user_name_attribute: response
  ```
- OAuthAttributes 클래스에 네이버인지 판단하는 코드 추가
  ```
   public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if("naver".equals(registrationId)){
            return ofNaver("id",attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String,Object> response= (Map<String, Object>) attributes.get("response");
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
  ```
#### 기존 테스트에 Security 추가하기
- test용 application.yml 추가
  ```
  spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test
            client-secret: test
            scope:
              - email
              - profile
  ```
- 기존 @Test에 @WithMockUser(roles="USER) 인증된 가짜 사용자를 만들어 사용한다.
- MockMvc를 사용하여 테스트를 구성한다. 

  


