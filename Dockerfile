# Step 1: Build stage
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run stage
# We switch from 'openjdk' to 'eclipse-temurin'
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]