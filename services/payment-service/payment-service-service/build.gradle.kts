plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    // git-properties отключён из-за несовместимости с Java 25
    // id("com.gorylenko.gradle-git-properties")
}

dependencies {
    implementation(project(":common:common-security"))
    implementation(project(":common:common-web"))
    implementation(project(":common:common-messaging"))
    implementation(project(":services:payment-service:payment-service-api"))
    implementation(project(":services:payment-service:payment-service-db"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // MapStruct
    val mapstructVersion: String by project
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    testImplementation(project(":common:common-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
