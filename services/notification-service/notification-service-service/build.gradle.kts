plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        val springCloudVersion: String by project
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-web"))
    implementation(project(":common:common-messaging"))
    implementation(project(":services:notification-service:notification-service-api"))
    implementation(project(":services:notification-service:notification-service-db"))
    implementation(project(":services:user-service:user-service-client"))
    implementation(project(":services:user-service:user-service-api"))
    implementation(project(":services:event-service:event-service-api"))
    implementation(project(":services:event-service:event-service-client"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OpenAPI / Swagger
    val openApiVersion: String by project
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$openApiVersion")

    // OpenFeign for inter-service communication
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Telegram Bot API
    val telegramBotApiVersion: String by project
    implementation("com.github.pengrad:java-telegram-bot-api:$telegramBotApiVersion")

    // Mustache Template Engine
    val jmustacheVersion: String by project
    implementation("com.samskivert:jmustache:$jmustacheVersion")

    // MapStruct
    val mapstructVersion: String by project
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    testImplementation(project(":common:common-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
