FROM maven:3.9.16-eclipse-temurin-26

WORKDIR /app

COPY pom.xml .
COPY testng.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mkdir -p /app/target/screenshots /app/target/extent /app/target/allure-results

ENV GRID_URL=http://selenium-hub:4444
ENV BROWSER=chrome
ENV BASE_URL=https://www.zigwheels.com
ENV USE_GRID=true

CMD ["sh", "-c", "mvn -B test ${TEST_GROUPS:+-Dtest.groups=$TEST_GROUPS}"]