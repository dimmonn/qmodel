FROM maven:3.9-amazoncorretto-17
WORKDIR /app
COPY . .
CMD ["mvn", "spring-boot:run"]