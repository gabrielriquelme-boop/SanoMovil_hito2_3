import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

group = "cl.sanomovil"
version = "0.2.0"

repositories {
    mavenCentral()
}

val ktorVersion = "3.2.0"

dependencies {
    // Servidor HTTP (Ktor) y sus plugins
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-server-forwarded-header")
    implementation("io.ktor:ktor-server-rate-limit")

    // Base de datos: pool de conexiones + driver de PostgreSQL (SQL explícito con JDBC)
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.postgresql:postgresql:42.7.5")

    // Seguridad: hash de contraseñas
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Registro de eventos
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("cl.sanomovil.backend.ApplicationKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
