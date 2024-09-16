plugins {
  kotlin("jvm") version "2.0.0"
  kotlin("plugin.serialization") version "2.0.0"
  id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
  id("jacoco")
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

val GROUP = "com.c0x12c.feature.flag"
var RELEASE_VERSION = "0.1.0"

if (System.getenv("RELEASE_VERSION") != null) {
  RELEASE_VERSION = System.getenv("RELEASE_VERSION")
  println("Release version: $RELEASE_VERSION")
}

group = GROUP
version = RELEASE_VERSION

repositories {
  mavenCentral()
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

jacoco {
  toolVersion = "0.8.12"
}

ktlint {
  android.set(false)
  outputToConsole.set(true)
  ignoreFailures.set(false)
  reporters {
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
  }
  filter {
    exclude("**/generated/**")
    include("**/src/main/kotlin/**")
    include("**/src/test/kotlin/**")
  }
}

sourceSets {
  main {
    kotlin.srcDirs("src/main/kotlin")
  }
  test {
    kotlin.srcDirs("src/test/kotlin")
  }
}

tasks.named("compileJava").configure {
  enabled = false
}
tasks.named("compileTestJava").configure {
  enabled = false
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")

  implementation("javax.inject:javax.inject:1")
  implementation("redis.clients:jedis:5.1.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  implementation("org.jetbrains.exposed:exposed-core:0.49.0")
  implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")
  implementation("org.jetbrains.exposed:exposed-dao:0.49.0")
  implementation("org.jetbrains.exposed:exposed-java-time:0.49.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
  implementation("org.postgresql:postgresql:42.4.1")
  implementation("net.postgis:postgis-jdbc:2.5.1")
  implementation("com.zaxxer:HikariCP:5.0.1")

  testImplementation(kotlin("test"))

  testImplementation("org.postgresql:postgresql:42.7.4")

  // JUnit
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")

  // MockK for mocking objects
  testImplementation("io.mockk:mockk:1.13.12")

  // Coroutines for suspend functions
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")

  testImplementation("org.testcontainers:testcontainers:1.20.1")
  testImplementation("org.testcontainers:postgresql:1.20.1")
  testImplementation("org.testcontainers:junit-jupiter:1.20.1")
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
  reports {
    junitXml.required.set(true)
  }
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

java {
  withJavadocJar()
  withSourcesJar()
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = "module-feature-flag"

      pom {
        name.set("Feature Flag Module")
        description.set("A module for managing feature flags")
        url.set("https://github.com/c0x12c/feature-flag-module")
        licenses {
          license {
            name.set("Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("spartan-dev")
            name.set("Spartan Dev")
            email.set("chan@c0x12c.com")
          }
        }
        scm {
          connection.set("scm:git:git://github.com/c0x12c/spartan-module-feature-flag-kotlin.git")
          developerConnection.set("scm:git:ssh://github.com:c0x12c/spartan-module-feature-flag-kotlin.git")
          url.set("https://github.com/c0x12c/spartan-module-feature-flag-kotlin")
        }
      }
    }
  }
}

signing {
  sign(publishing.publications["mavenJava"])
}
