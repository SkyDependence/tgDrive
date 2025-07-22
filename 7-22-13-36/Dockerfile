# 设置基础镜像 - 改为 Java 17
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录为 /app
WORKDIR /app

# 将 JAR 文件复制到工作目录
COPY target/STdrive-0.1.jar app.jar

# 暴露端口（根据你的应用端口）
EXPOSE 8085

# 容器启动时运行的命令
CMD ["java", "-jar", "app.jar"]
