plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        val springBootVersion: String by project
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

val testcontainersVersion: String by project
val datafakerVersion: String by project
val allureVersion: String by project

dependencies {
    api(project(":common:common-api"))
    api(project(":common:common-security"))

    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.security:spring-security-test")

    // Allure - api чтобы был доступен в тестах сервисов
    api("io.qameta.allure:allure-junit5:$allureVersion")

    // Jackson - для TestLogger.attachJson()
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Testcontainers
    api("org.testcontainers:junit-jupiter:$testcontainersVersion")
    api("org.testcontainers:postgresql:$testcontainersVersion")
    api("org.testcontainers:rabbitmq:$testcontainersVersion")

    // Data Faker - генерация тестовых данных
    api("net.datafaker:datafaker:$datafakerVersion")

    // REST Assured
    api("io.rest-assured:rest-assured")
    api("io.rest-assured:spring-mock-mvc")
}
