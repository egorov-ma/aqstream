pluginManagement {
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val gitPropertiesVersion: String by settings
    val allurePluginVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
        id("com.gorylenko.gradle-git-properties") version gitPropertiesVersion
        id("io.qameta.allure") version allurePluginVersion
    }
}

rootProject.name = "aqstream"

// Common modules
include("common:common-api")
include("common:common-security")
include("common:common-data")
include("common:common-messaging")
include("common:common-web")
include("common:common-test")

// Gateway (single module - WebFlux based)
include("services:gateway")

// User Service
include("services:user-service:user-service-api")
include("services:user-service:user-service-service")
include("services:user-service:user-service-db")
include("services:user-service:user-service-client")

// Event Service
include("services:event-service:event-service-api")
include("services:event-service:event-service-service")
include("services:event-service:event-service-db")
include("services:event-service:event-service-client")

// Payment Service
include("services:payment-service:payment-service-api")
include("services:payment-service:payment-service-service")
include("services:payment-service:payment-service-db")

// Notification Service
include("services:notification-service:notification-service-api")
include("services:notification-service:notification-service-service")
include("services:notification-service:notification-service-db")

// Media Service
include("services:media-service:media-service-api")
include("services:media-service:media-service-service")
include("services:media-service:media-service-db")

// Analytics Service
include("services:analytics-service:analytics-service-api")
include("services:analytics-service:analytics-service-service")
include("services:analytics-service:analytics-service-db")
