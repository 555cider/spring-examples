import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("org.springframework.boot") version "4.0.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    repositories {
        mavenCentral()
    }

    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure(DependencyManagementExtension::class.java) {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.0")
            mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
        }
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnitPlatform()
    }
}
