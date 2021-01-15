# 스프링 부트와 AWS로 혼자 구현하는 웹 서비스

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

---

### 6장
#### AWS 설정하기
- 포트 항목이 22인 경우: AWS를 EC2 터미널로 접속하는 것을 의미
  - 보안 그룹에서 ssh접속은 지정된 IP에서만 ssh접속이 가능하도록 구성하는 것이 안전하다. 
- 고정 IP 등록: 고정 IP 할당을 하지 않으면 인스턴스의 IP가 인스턴스를 중지하고 다시 시작하면 변경된다.
  - EIP 할당(탄력적 IP 할당): 탄력적 IP를 발급 받고 EC2 주소를 연결한다.(작업 -> 주소 연결 선택) 
  - 탄력적 IP를 생성하고 EC2에 바로 연결하지 않으면 비용 청구가 된다. 탄력적 IP를 사용할 인스턴스가 없다면 탄력적 IP를 삭제해야한다.
  
#### putty 사용
- puttygen을 통해 pem키를 ppk파일로 변환해야한다.
- putty HostName: username@public_ip ex)ec2-user@탄력적 IP 주소
- Connection -> SSH -> Auth 를 들어가서 ppk 파일 등록
- Session에서 현재 설정들 저장

### 아마존 리눅스 서버 생성 시 꼭 해야 할 설정들
- java 설정
  ```
  sudo yum install -y java-1.8.0-openjdk-devel.x86_64 //java 8 설치
  ```
  ```
  sudo /usr/sbin/alternatives --config java //java 버전 선택
  ```
  ```
  sudo yum remove java-1.7.0-openjdk //사용하지 않는 java버전 삭제
  java -version //현재 버전 확인
  ```
- 타임존 변경
  ```
  sudo rm /etc/localtime
  sudo ln -s /usr/share/zoneinfo/Asia/Seoul /etc/localtime
  date //타임존 확인
  ```
- Hostname 변경
  ```
  sudo hostnamectl set-hostname webserver.mydomain.com
  sudo reboot //재부팅후 hostname 변경 확인
  ```
  ```
  sudo vim /etc/hosts
  ```
  위의 명령어로 파일을 열고 아래 설정 추가
  ```
  127.0.0.1   등록한 HOSTNAME
  ```
  등록한 호스트이름 확인
  ```
  curl 등록한 호스트 이름
  ```
  
---

### 7장
#### RDS 인스턴스 생성하기
- 표준 생성 MariaDB 선택, 퍼블릭 엑세스 기능 체크
- 운영 환경에 맞는 파라미터 설정하기
  - 파라미터 그룹 -> 파라미터 그룹 생성 -> 생성 완료 후 생성된 파라미터 그룹을 클릭해서 파라미터 편집  
  - time_zone 검색하여 Asia/Seoul 선택
  - character_set 설정 (utf8은 이모지를 저장할 수 없다.)
    * character_set_client -> utf8mb4
    * character_set_connection -> utf8mb4
    * character_set_database -> utf8mb4
    * character_set_filesystem -> utf8mb4
    * character_set_results -> utf8mb4
    * character_set_server -> utf8mb4
    * collation_connection -> utf8mb4_general_ci
    * collation_server -> utf8mb4_general_ci
  - max_connections -> 150
  - 파라미터 설정을 마쳤다면 데이터베이스에 연결(데이터베이스 -> 수정 -> DB 파라미터 그룹수정)
  - 파라미터 설정이 제대로 반영되지 않을 수도 있기 때문에 인스턴스 재부팅을 한다.
- 보안 그룹 목록 중 EC2에 사용된 보안 그룹의 그룹 ID를 복사하고 RDS 보안 그룹의 인바운드에 복사된 보안 그룹 ID와 본인의 IP를 추가한다.
#### 인텔리제이 Database 플러그인 설치 및 사용
- Database Navigator 설치후 Action(DB Browser)를 들어가서 실행시킨다.
- DB Browser에 추가버튼을 클릭하고 MySQL을 선택한다.
- 자신이 생성한 RDS 정보를 등록한다. (Host = RDS 엔드 포인트)
- DB character_set,collation 설정 확인
  ```
  use 데이터베이스명
  show variables like 'c%';
  ```
  character_set_database, collation_connection 두 항목이 latin1로 되어있을 경우
  ```
  ALTER DATABASE 데이터베이스명
  CHARACTER SET ='utf8mb4'
  COLLATE = 'utf8mb4_general_ci'
  ```
  ```
  select @@time_zone,now(); //타임존 확인
  ```
- 설정을 마친 후 mysql 문법을 사용하여 데이터베이스를 사용하면 된다.
- EC2에서 RDS 접근 확인
  ```
  sudo yum install mysql
  mysql -u 계정 -p -h Host주소
  show databases;
  ```
  
---

### 8장
#### EC2서버에 프로젝트 배포하기
- EC2에 프로젝트 clone 받기
  ```
  sudo yum install git
  git --version
  mkdir ~/app && mkdir ~/app/step1
  cd ~/app/step1
  git clone 깃허브 repository clone 주소
  ```
- 테스트 수행
  ```
  git pull
  chmod +x ./gradlew //실행 권한 추가
  ./gradlew test
  ```
- 배포 스크립트 만들기
  - git clone or git pull 을 통해 새 버전의 프로젝트 받기
  - Gradle이나 Maven을 통해 프로젝트 테스트와 빌드
  - EC2서버에서 해당 프로젝트 실행 및 재실행 
  - vim ~/app/step1/deploy.sh 파일
  ```
   #!/bin/bash
  
  REPOSITORY=/home/ec2-user/app/step1
  PROJECT_NAME=freelec-springboot2-webservice
  
  cd $REPOSITORY/$PROJECT_NAME/
  
  echo "> Git pull"
  
  git pull
  
  echo "> 프로젝트 Build 시작"
  
  ./grdlew build
  
  echo "> step1 디렉토리로 이동"
  
  cd $REPOSITORY
  
  echo "> Build 파일 복사"
  
  cp $REPOSITORY/$PROJECT_NAME/build/libs/*.jar $REPOSITORY/
  
  echo "> 현재 구동중인 애플리케이션 pid 확인"
  
  CURRENT_PID=$(pgrep -f ${PROJECT_NAME}.*.jar)
  //pgrep은 process id만 추출하는 명령어이다.
  //-f 옵션은 프로세스 이름으로 찾는다.
  echo "현재 구동중인 어플리케이션 pid: $CURRENT_PID"
  
  if [ -z "$CURRENT_PID" ]; then
  echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다."
  else
  echo "> kill -15 $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
  fi
  
  echo "> 새 어플리케이션 배포"
  
  JAR_NAME=$(ls -tr $REPOSITORY/ | grep jar | tail -n 1)
  //새로 시작할 jar 파일명을 찾는다.
  //여러 jar 파일이 생기기 때문에 tail -n으로 가장 나중의 jar 파일을 변수에 저장한다.
  echo "> JAR Name: $JAR_NAME"
  
  nohup java -jar $REPOSITORY/$JAR_NAME 2>&1 &
  //일반적으로 java -jar 명령어로 사용하면 터미널 접속이 끊어지면 애플리케이션도 종료된다.
  //nohup 사용으로 터미널을 종료해도 애플리케이션이 구동되도록 한다.
  ```
  ```
  chmod +x ./deploy.sh // 실행 권한 추가
  ./deploy.sh //스크립트 실행
  ```
- 외부 secret 파일 등록
  - 서버에서 직접 설정을 가지고 있도록 한다.
    ```
    vim /home/ec2-user/app/application-oauth.properties
    ```  
  - deploy.sh 파일수정
    ```
    nohup java -jar \
      -Dspring.config.location=classpath:/application.properties,/home/ec2-user/app/application-oauth.properties \
      $REPOSITORY/$JAR_NAME 2>&1 &
    ```
- RDS 테이블 생성 및 프로젝트 설정
  - JPA가 사용될 엔티티 테이블과 스프링 세션이 사용될 테이블 2가지 종류를 생성함. (스프링 세션 테이블은 schema-mysql.sql 파일에서 확인할 수 있다.)
  - compile("org.mariadb.jdbc:mariadb-java-client") 추가
  - application-real.properties 추가
    ```
    spring.profiles.include=oauth,real-db
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
    spring.session.store-type=jdbc
    ```
  - EC2 서버에 application-real-db.properties 생성
    ```
    spring.jpa.hibernate.ddl-auto=none
    spring.datasource.url=jdbc:mariadb://rds주소:포트명(기본은 3306)/database명
    spring.datasource.username=db계정
    spring.datasource.password=db계정 비밀번호
    spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
    ```
  -  deploy.sh 파일 수정
    ```
    nohup java -jar \
      -Dspring.config.location=classpath:/application.properties,classpath:/application-real.properties,/home/ec2-user/app/application-oauth.properties,/home/ec2-user/app/application-real-db.properties \
      -Dspring.profiles.active=real \
      $REPOSITORY/$JAR_NAME 2>&1 &
    ```
  - deploy.sh 실행후 
    ```
    curl localhost:8080
    ```
    실행 html 코드가 보인다면 성공.
#### EC2에서 소셜로그인
- ec2 보안그룹에서 8080이 열려있도록 한다.
- 퍼블릭 DNS에 :8080을 붙여서 구글 웹 콘솔에서 승인된 리디렉션 URI 추가, 네이버에 EC2 주소 등록
- 네이버의 서비스 URL은 8080포트를 제외하고 실제 도메인 주소만 입력한다.

--- 

### 9장
#### CI & CD
- CI란 VCS 시스템에 PUSH가 되면 자동으로 테스트와 빌드가 수행되어 안정적인 배포 파일을 만드는 과정을 CI라고 한다.
- CD란 빌드 결과를 자동으로 운영 서버에 무중단 배포까지 진행되는 과정이다.
#### Travis CI 연동
- 계정 -> setting -> 깃허브 저장소 활성화
- .travis.yml 파일 설정
  ```
  language: java
  jdk:
    - openjdk8
  branches:
    only:
      - master
  # Travis CI 서버의 Home
  cache:
    directories:
      - '$HOME/.m2/repository'
      - '$HOME/.gradle'
  
  script: "./gradlew clean build"
  # CI 실행 완료시 메일로 알람
  notifications:
    email:
      recipients:
        - 본인 이메일 주소
  ```
#### Travis CI와 AWS S3 연동
- S3란 AWS에서 제공하는 일종의 파일 서버이다. 이미지 업로드 구현한다면 S3를 이용하여 구현하는 경우가 많다.
- Travis CI가 S3로 jar를 전달한다.
- 배포할 때 사용할 CodeDeploy는 저장 기능이 없기 때문에 Travis CI가 빌드한결과물을 받아서 CodeDeploy가 가져갈 수 있도록 보관할 수 있는 공간이 ASWS S3이다.
- 연동 과정
  - AWS key 발급 (AWS서비스에 외부 서비스가 접근할 수 없기 때문에 접근 가능한 권한을 가진 key를 생성해야한다.)
    - IAM을 사용하여 Travis CI가 AWS의 S3와 CodeDeploy에 접근할 수 있도록 한다.
    - IAM 검색 -> 사용자 -> 사용자 추가 -> 사용자의 이름과 프로그래밍 방식 엑세스 선택 -> 기존 정책 직접 연결 -> s3full검색하여 권한 추가 codedeployf 검색하여 권한 추가
  - 엑세스 키 ID와 비밀 엑세스 키를 Travis CI에 등록한다. 
    - TravisCi 설정 -> AWS_ACCESS_KEY : 엑세스 키 ID , AWS_SECRET_KEY: 비밀 엑세스 키 등록
    - 여기에 등록된 값은 .travis.yml에서 $AWS_ACCESS_KEY,$AWS_SECRET_KEY로 사용가능
- S3 버킷 생성  
  - 퍼블릭 엑세스 모두 차단하여 만든다.
- Travis CI에서 빌드하여 만든 jar파일을 S3에 올릴 수 있도록 .travis.yml파일 수정
  ```
  language: java
  jdk:
  - openjdk8
  
  branches:
  only:
  - master
  # Travis CI 서버의 Home
  cache:
  directories:
  - '$HOME/.m2/repository'
  - '$HOME/.gradle'
  
  script: "./gradlew clean build"
  
  before_install:
  - chmod +x gradlew
  # CodeDeploy는 Jar파일을 인식하지 못하므로 Jar+ 기타 설정 파일들을 모아 압축한다.
  before_deploy:
    - zip -r freelec-springboot2-webservice *
    - mkdir -p deploy
    - mv freelec-springboot2-webservice.zip deploy/freelec-springboot2-webservice.zip 
  deploy:
  - provider: s3
    access_key_id: $AWS_ACCESS_KEY
    secret_access_key: $AWS_SECRET_KEY
    bucket: freelec-springboot-webserver-build # S3 버킷
    region: ap-northeast-2
    skip_cleanup: true
    acl: private # zip 파일 접근을 private으로
    # 앞에서 생성한 deploy 디렉토리 지정, 해당 위치의 파일들만 S3로 전송
    local_dir: deploy #before_deploy에서 생성한 디렉토리
    wait-until-deployed: true
  
  # CI 실행 완료시 메일로 알람
  notifications:
  email:
  recipients:
  - 본인 메일 주소
  ```
#### Travis CI, AWS S3 ,CodeDeploy 연동
- EC2가 CodeDeploy를 연동 받을 수 있게 IAM 역할 추가
  - EC2에서 사용할 것이기 때문에 역할로 처리
  - 정책에서 EC2RoleForA를 검색하여 추가
  - EC2 인스턴스 설정 -> IAM 역할 바꾸기 -> 재부팅
- CodeDeploy의 요청을 받을 수 잇게 에이전트 설치
  ```
  aws s3 cp s3://aws-codedeploy-ap-northeast-2/latest/install .
  --region ap-northeast-2
  chmod +x ./install 
  sudo ./install auto
  ```
  ```
  sudo service codedeploy-agent status
  ```
- CodeDeploy에서 EC2에 접근하기 위해 권한생성
  - IAM 역할 생성 -> AWS 서비스 -> CodeDeploy
#### CodeDeploy 생성  
- 컴퓨팅 플랫폼에선 EC2/온프레미스 선택
- 배포 그룹 생성  -> 배포 그룹 이름 , 서비스 역할 선택(CodeDeploy용 IAM 역할) 
  - 배포 구성 : CodeDeployDefault.AllAtOnce
  - 로드 밸런싱 활성화 해제
- Travis CI, S3, CodeDeploy 연동
  ```
  mkdir ~/app/step2 && mkdir ~/app/step2/zip
  ```
  - appspec.yml 파일 생성
  ```
  version: 0.0
  os: linux
  //source는 CodeDeploy에서 전달해준 파일 중 destination으로 이동시킬 대상을 지정함.
  //이후 jar 를 실행하는 등은 destination에서 옮긴 파일들롲 진행됨.
  files:
  - source: /
    destination: /home/ec2-user/app/step2/zip/
    overwrite: yes
  ```
  - .travis.yml 파일에 CodeDeploy 내용 추가
  ```
  ...
  - provider: codedeploy
    access_key_id: $AWS_ACCESS_KEY
    secret_access_key: $AWS_SECRET_KEY
    bucket: freelec-springboot-webserver-build
    key: freelec-springboot2-webservice.zip # 빌드 파일 압축해서 전달

    bundle_type: zip # 압축 확장자
    application: freelec-springboot2-webservice # 웹 콘솔에서 등록한 CodeDeploy 애플리케이션
    deployment_group: freelec-springboot2-webservice-group # 웹 콘솔에서 등록한 CodeDeploy 배포그룹
    region: ap-northeast-2
    wait-until-deployed: true
  ```
  - 프로젝트를 커밋하고 푸시하면 Travis CI가 자동으로 시작되고 Travis CI가 끝나면 CodeDeploy 배포가 수행된다.
  - 배포가 끝났다면 /home/ec2-user/app/step2/zip 에 파일목록들을 확인해본다.
#### 배포 자동화 구성
- script 디렉토리를 생성하고 deploy.sh 를 생성한다.
  ```
  #!/bin/bash
  
  REPOSITORY=/home/ec2-user/app/step2
  PROJECT_NAME=freelec-springboot2-webservice
  
  echo "> Build 파일 복사"
  
  cp $REPOSITORY/zip/*.jar $REPOSITORY/
  
  echo "> 현재 구동중인 애플리케이션 pid 확인"
  
  CURRENT_PID=$(pgrep -fl freelec-springboot2-webservice | grep jar | awk '{print $1}')
  
  echo "현재 구동중인 어플리케이션 pid: $CURRENT_PID"
  
  if [ -z "$CURRENT_PID" ]; then
      echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다."
  else
      echo "> kill -15 $CURRENT_PID"
      kill -15 $CURRENT_PID
      sleep 5
  fi
  
  echo "> 새 어플리케이션 배포"
  
  JAR_NAME=$(ls -tr $REPOSITORY/*.jar | tail -n 1)
  
  echo "> JAR Name: $JAR_NAME"
  
  echo "> $JAR_NAME 에 실행권한 추가"
  
  chmod +x $JAR_NAME
  
  echo "> $JAR_NAME 실행"
  nohup java -jar \
      -Dspring.config.location=classpath:/application.properties,classpath:/application-real.properties,/home/ec2-user/app/application-oauth.properties,/home/ec2-user/app/application-real-db.properties \
      -Dspring.profiles.active=real \
      $JAR_NAME > $REPOSITORY/nohup.out 2>&1 &
  ```
- .travis.yml 파일 수정
  ```
  before_deploy:
    - mkdir -p before-deploy # zip에 포함시킬 파일들을 담을 디렉토리 생성
    - cp scripts/*.sh before-deploy/
    - cp appspec.yml before-deploy/
    - cp build/libs/*.jar before-deploy/
    - cd before-deploy && zip -r before-deploy * # before-deploy로 이동후 전체 압축
    - cd ../ && mkdir -p deploy # 상위 디렉토리로 이동후 deploy 디렉토리 생성
    - mv before-deploy/before-deploy.zip deploy/freelec-springboot2-webservice.zip # deploy로 zip파일 이동
  ```
- appspec.yml 파일 수정 
```
permission:
  - object: /
    pattern: "**"
    owner: ec2-user
    group: ec2-user

hooks:
  ApplicationStart:
    - location: deploy.sh 
      timeout: 60
      runas: ec2-user
```

---




  

  



