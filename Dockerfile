# lightweight Java 21 image
FROM eclipse-temurin:21-jre-alpine

# working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]