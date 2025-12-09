plugins {
    `java-library`
}

// Версии совместимы со Spring Boot 3.5.x BOM
val jakartaValidationVersion = "3.0.2"
val jacksonVersion = "2.18.2"

dependencies {
    api("jakarta.validation:jakarta.validation-api:$jakartaValidationVersion")
    api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
}
