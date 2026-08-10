# Maven + Java21이 이미 설치된 이미지를 가져오는 것 그리고 builder라는 이름으로 참조하겠다
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# 컨테이너 안에서 앞으로 명령을 실행할 작업 폴더 (Docker에 있는 별개의 작업 폴더)
WORKDIR /workspace

# pom.xml에 있는 dependency를 미리 받아놓음
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

# 실제 소스 코드를 복사해서 JAR를 만듬
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

# 새로운 깨끗한 이미지 실제 서버에는 Maven이 필요없으니까
FROM eclipse-temurin:21-jre-alpine

# Linux 사용자 cruxmate를 만듬
RUN addgroup -S cruxmate && adduser -S cruxmate -G cruxmate

WORKDIR /app

# 위에서 나온 결과물인 jar 하나만 가져옴
COPY --from=builder /workspace/target/cruxmate-*.jar app.jar

USER cruxmate

# 컨테이너 내부에서 8080 사용
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
