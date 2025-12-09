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
    api(project(":common:common-security"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenAPI / Swagger
    val openApiVersion: String by project
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$openApiVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
