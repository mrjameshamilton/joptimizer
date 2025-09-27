plugins {
    id("java")
    id("com.diffplug.spotless") version "8.0.0"
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
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation(libs.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit.jupiter)
    testRuntimeOnly(libs.bundles.junit.runtime)
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.28.0").aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        forbidWildcardImports()
    }
}

tasks.test {
    useJUnitPlatform()
}

