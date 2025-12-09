rootProject.name = "aqstream"

// Common modules
include("common:common-api")
include("common:common-security")
include("common:common-data")
include("common:common-messaging")
include("common:common-web")
include("common:common-test")

// Services
include("services:gateway")
include("services:user-service")
include("services:event-service")
include("services:payment-service")
include("services:notification-service")
include("services:media-service")
include("services:analytics-service")
