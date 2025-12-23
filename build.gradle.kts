plugins {
    java
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("checkstyle")
    id("jacoco")
}

group = "ru.aqstream"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // test — все тесты (unit + integration + e2e)
    tasks.named<Test>("test") {
        description = "Runs all tests (unit + integration + e2e)."
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    // unit — только unit тесты (без integration и e2e)
    tasks.register<Test>("unit") {
        description = "Runs unit tests only."
        group = "verification"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            excludeTags("integration", "e2e")
        }
    }

    // integration — только интеграционные тесты
    tasks.register<Test>("integration") {
        description = "Runs integration tests only."
        group = "verification"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags("integration")
        }
        shouldRunAfter(tasks.named("unit"))
    }

    // e2e — только E2E тесты
    tasks.register<Test>("e2e") {
        description = "Runs end-to-end tests only."
        group = "verification"
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags("e2e")
        }
        shouldRunAfter(tasks.named("integration"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required = true
            html.required = true
        }
    }

    checkstyle {
        toolVersion = "10.21.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
    }

    val lombokVersion: String by project
    val junitVersion: String by project
    val allureVersion: String by project

    dependencies {
        "compileOnly"("org.projectlombok:lombok:$lombokVersion")
        "annotationProcessor"("org.projectlombok:lombok:$lombokVersion")

        "testCompileOnly"("org.projectlombok:lombok:$lombokVersion")
        "testAnnotationProcessor"("org.projectlombok:lombok:$lombokVersion")

        "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")

        // Allure
        "testImplementation"("io.qameta.allure:allure-junit5:$allureVersion")
    }

    // Настройка systemProperty для Allure
    tasks.withType<Test> {
        systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
    }
}

// Корневой отчёт JaCoCo для всех модулей
tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })

    additionalSourceDirs.setFrom(subprojects.flatMap { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.flatMap { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.flatMap { it.sourceSets.main.get().output })
    executionData.setFrom(subprojects.mapNotNull {
        val file = it.layout.buildDirectory.file("jacoco/test.exec").get().asFile
        if (file.exists()) file else null
    })

    reports {
        xml.required = true
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/html")
    }
}

// Корневой отчёт Allure для всех модулей
tasks.register("allureAggregateReport") {
    group = "verification"
    description = "Generates aggregated Allure report for all subprojects."
    dependsOn(subprojects.map { it.tasks.named("test") })

    doLast {
        // Allure результаты собираются из build/allure-results каждого модуля
        println("Allure results collected. Run './gradlew allureServe' to view the report.")
    }
}
