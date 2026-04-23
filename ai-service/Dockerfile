# Sử dụng JRE nhẹ thay vì JDK+Maven để giảm kích thước image (từ ~600MB xuống ~200MB)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy file JAR đã được build từ bước CI trước đó
# Lưu ý: file JAR được copy từ thư mục target/ của service tương ứng
COPY target/*.jar app.jar

# Cấu hình các biến môi trường mặc định (có thể override khi chạy)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8087

# Chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
