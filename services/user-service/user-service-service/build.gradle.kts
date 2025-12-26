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

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-web"))
    implementation(project(":common:common-messaging"))
    implementation(project(":services:user-service:user-service-api"))
    implementation(project(":services:user-service:user-service-db"))

    implementation("org.springframework.boot:spring-boot-starter-web")
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

    testImplementation(project(":common:common-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
