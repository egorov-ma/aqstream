plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        val springBootVersion: String by project
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        val springCloudVersion: String by project
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    api(project(":services:event-service:event-service-api"))

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}
