plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    // git-properties отключён из-за несовместимости с Java 25
    // id("com.gorylenko.gradle-git-properties")
}

val mapstructVersion: String by project
val lombokVersion: String by project
val lombokMapstructBindingVersion: String by project
val openApiVersion: String by project
val zxingVersion: String by project

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
    implementation(project(":services:event-service:event-service-api"))
    implementation(project(":services:event-service:event-service-db"))

    // Feign client для user-service
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation(project(":services:user-service:user-service-client"))
    implementation(project(":services:user-service:user-service-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$openApiVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // MapStruct + Lombok
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // QR Code Generation
    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.google.zxing:javase:$zxingVersion")

    testImplementation(project(":common:common-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
