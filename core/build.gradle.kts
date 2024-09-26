import com.c0x12c.featureflag.dependency.Libraries

plugins {
  kotlin("jvm")
//  kotlin("plugin.serialization") version "1.9.23"
//  id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
//  id("maven-publish")
//  id("signing")
//  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
//  id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.c0x12c.featureflag"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")

  implementation("javax.inject:javax.inject:1")
  implementation("redis.clients:jedis:5.1.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  implementation("org.jetbrains.exposed:exposed-core:0.49.0")
  implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")
  implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
  implementation("org.jetbrains.exposed:exposed-java-time:0.49.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
//  implementation("org.postgresql:postgresql:42.4.1")
//  implementation("net.postgis:postgis-jdbc:2.5.1")
  implementation("com.zaxxer:HikariCP:5.0.1")
  implementation("com.goncalossilva:murmurhash:0.4.0")
  implementation("org.apache.maven:maven-artifact:3.9.9")

  implementation(Libraries.Logging.SLF4J)

  testImplementation(kotlin("test"))

  testImplementation("org.postgresql:postgresql:42.7.4")

  // JUnit
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")

  // MockK for mocking objects
  testImplementation("io.mockk:mockk:1.13.12")

  // Coroutines for suspend functions
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")

  testImplementation("org.testcontainers:testcontainers:1.20.1")
  testImplementation("org.testcontainers:postgresql:1.20.1")
  testImplementation("org.testcontainers:junit-jupiter:1.20.1")
}
