plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("com.github.johnrengelman.shadow") version Versions.SHADOW_JAR
}

group = "hub.nebula"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // ===[ Kotlin Stuff ]===
    testImplementation(kotlin("test"))

    // ===[ Logging Stuff ]===
    implementation("io.github.microutils:kotlin-logging:${Versions.KOTLIN_LOGGING}")
    implementation("org.slf4j:slf4j-api:${Versions.SLF4J}")
    implementation("org.slf4j:slf4j-simple:${Versions.SLF4J}")

    // ===[ Discord Stuff ]===
    implementation("net.dv8tion:JDA:${Versions.JDA}")
    implementation("club.minnced:jda-ktx:${Versions.JDA_KTX}")

    // ===[ Database Stuff ]===
    implementation("com.zaxxer:HikariCP:${Versions.HIKARI}")
    implementation("org.jetbrains.exposed:exposed-core:${Versions.EXPOSED}")

    // ===[ Serialization Stuff ]===
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${Versions.KOTLINX_SERIALIZATION}")
}

tasks {
    shadowJar {
        archiveBaseName.set("nebula-pangea")
        archiveVersion.set(version.toString())
        archiveClassifier.set("")
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(17)
}