plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("com.github.johnrengelman.shadow") version Versions.SHADOW_JAR
}

group = "hub.nebula"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // For Lavalink
    maven("https://maven.arbjerg.dev/snapshots")
    maven("https://maven.topi.wtf/releases")
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // ===[ Kotlin Stuff ]===
    testImplementation(kotlin("test"))

    // ===[ Logging Stuff ]===
    implementation("io.github.microutils:kotlin-logging:${Versions.KOTLIN_LOGGING}")
    implementation("org.slf4j:slf4j-api:${Versions.SLF4J}")
    implementation("org.slf4j:slf4j-simple:${Versions.SLF4J}")

    // ===[ Discord & Music Stuff ]===
    implementation("net.dv8tion:JDA:${Versions.JDA}")
    implementation("club.minnced:jda-ktx:${Versions.JDA_KTX}")
    implementation("dev.schlaubi.lavakord:jda:${Versions.LAVAKORD_JDA}")
    implementation("com.github.topi314.lavasrc:lavasrc:${Versions.LAVASRC}")
    implementation("com.github.topi314.lavasrc:lavasrc-protocol:${Versions.LAVASRC}")
    implementation("com.github.topi314.lavasearch:lavasearch:${Versions.LAVASEARCH}")

    // ===[ JVM Stuff ]===
    implementation("com.github.ben-manes.caffeine:caffeine:${Versions.CAFFEINE}")

    // ===[ Database Stuff ]===
    implementation("com.zaxxer:HikariCP:${Versions.HIKARI}")
    implementation("org.jetbrains.exposed:exposed-core:${Versions.EXPOSED}")
    implementation("org.jetbrains.exposed:exposed-dao:${Versions.EXPOSED}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${Versions.EXPOSED}")
    implementation("org.postgresql:postgresql:${Versions.PSQL}")

    // ===[ Serialization Stuff ]===
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${Versions.KOTLINX_SERIALIZATION}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.JACKSON}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON}")
}

tasks {
    shadowJar {
        archiveBaseName.set("nebula-pangea")
        archiveVersion.set(version.toString())
        archiveClassifier.set("")

        manifest {
            attributes["Main-Class"] = "hub.nebula.pangea.PangeaLauncher"
        }
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(17)
}