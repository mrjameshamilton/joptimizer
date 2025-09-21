plugins {
    id("java")
}

group = "eu.jameshamilton"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit.jupiter)
    testRuntimeOnly(libs.bundles.junit.runtime)
}

tasks.test {
    useJUnitPlatform()
}

