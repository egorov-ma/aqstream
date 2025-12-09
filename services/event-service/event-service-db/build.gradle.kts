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
    api(project(":common:common-data"))
    api(project(":services:event-service:event-service-api"))

    testImplementation(project(":common:common-test"))
}
