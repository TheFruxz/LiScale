val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
	kotlin("jvm") version "1.8.0"
	id("io.ktor.plugin") version "2.2.2"
	id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
}

group = "de.fruxz.liscale"
version = "0.0.1"

application {
	mainClass.set("de.fruxz.ApplicationKt")

	val isDevelopment: Boolean = project.ext.has("development")
	applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
	mavenCentral()
	mavenLocal()
	maven("https://jitpack.io")
}

dependencies {

	// Ktor Server

	implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
	implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
	implementation("io.ktor:ktor-server-sessions-jvm:$ktor_version")
	implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
	implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
	implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
	implementation("ch.qos.logback:logback-classic:$logback_version")

	// MoltenKT

	implementation("com.github.TheFruxz:Ascend:18.0.0")

	// KotlinX

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")


	// Exposed

	implementation("org.jetbrains.exposed:exposed-core:0.40.1")
	implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")

	// SQLite JDBC

	implementation("org.xerial:sqlite-jdbc:3.40.0.0")

	// Test

	testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

}

kotlin {
	jvmToolchain(17)
}