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

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")

    // Database
    val postgresqlVersion: String by project
    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")

    // Liquibase
    val liquibaseVersion: String by project
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Testcontainers
    val testcontainersVersion: String by project
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}
