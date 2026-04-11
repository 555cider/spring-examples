pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "spring-projects"

include("discovery")
include("gateway")
include("auth")
include("client")
