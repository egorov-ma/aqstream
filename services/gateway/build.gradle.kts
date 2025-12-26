plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    // git-properties отключён из-за несовместимости с Java 25
    // id("com.gorylenko.gradle-git-properties")
}

dependencyManagement {
    imports {
        val springCloudVersion: String by project
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

val openApiVersion: String by project

dependencies {
    implementation(project(":common:common-api"))

    // Spring Cloud Gateway (WebFlux-based — единственное исключение из правила Spring MVC)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI / Swagger (WebFlux version)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$openApiVersion")

    // Redis для rate limiting
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // JWT — прямая зависимость, т.к. common-security использует servlet stack
    val jjwtVersion: String by project
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    // DataFaker для генерации тестовых данных
    val datafakerVersion: String by project
    testImplementation("net.datafaker:datafaker:$datafakerVersion")
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
