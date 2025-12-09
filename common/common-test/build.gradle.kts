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

dependencies {
    api(project(":common:common-api"))

    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.security:spring-security-test")

    // Testcontainers
    val testcontainersVersion: String by project
    api("org.testcontainers:junit-jupiter:$testcontainersVersion")
    api("org.testcontainers:postgresql:$testcontainersVersion")
    api("org.testcontainers:rabbitmq:$testcontainersVersion")

    // REST Assured
    api("io.rest-assured:rest-assured")
    api("io.rest-assured:spring-mock-mvc")
}
