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

// Версии из gradle.properties для совместимости с модулями без dependency-management плагина
val jakartaValidationVersion: String by project
val jacksonVersion: String by project
val springDataVersion: String by project

dependencies {
    api("jakarta.validation:jakarta.validation-api:$jakartaValidationVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")

    // Для PageResponse — только интерфейс Page, без полной реализации Spring Data
    compileOnly("org.springframework.data:spring-data-commons:$springDataVersion")
}
