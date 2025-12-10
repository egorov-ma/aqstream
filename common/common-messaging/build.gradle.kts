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

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.awaitility:awaitility:4.2.2")

    // Testcontainers
    val testcontainersVersion: String by project
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:rabbitmq:$testcontainersVersion")
}
