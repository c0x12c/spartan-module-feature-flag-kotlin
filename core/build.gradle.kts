import com.c0x12c.featureflag.dependency.Libraries

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "1.9.24"

  id("org.jetbrains.kotlinx.kover")

  `maven-publish`
  signing
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(Libraries.Kotlinx.KOTLINX_COROUTINES_CORE)
  implementation(Libraries.Kotlinx.KOTLINX_DATETIME)
  implementation(Libraries.Kotlinx.KOTLINX_SERIALIZATION_JSON)
  implementation(Libraries.Retrofit2.RETROFIT)
  implementation(Libraries.Retrofit2.CONVERTER_GSON)

  implementation(Libraries.JavaxInject.INJECT)

  implementation(Libraries.Exposed.CORE)
  implementation(Libraries.Exposed.DAO)
  implementation(Libraries.Exposed.JAVA_TIME)
  implementation(Libraries.Exposed.JDBC)

  implementation(Libraries.Jackson.KOTLIN)
  implementation(Libraries.Zaxxer.HIKARI)
  implementation(Libraries.Goncalossilva.MURMURHASH)
  implementation(Libraries.Maven.ARTIFACT)

  // Logging
  implementation(Libraries.Logging.LOGBACK_CLASSIC)
  implementation(Libraries.Logging.SLF4J)

  // Test
  testImplementation(kotlin("test"))

  // PostgreSQL
  testImplementation(Libraries.PostgreSQL.POSTGRESQL)

  // JUnit
  testImplementation(Libraries.Junit5.API)
  testImplementation(Libraries.Junit5.ENGINE)

  // MockK for mocking objects
  testImplementation(Libraries.Mockk.MOCKK)

  // Coroutines for suspend functions
  testImplementation(Libraries.Kotlinx.KOTLINX_COROUTINES_TEST)

  // Jedis
  testImplementation(Libraries.Redis.JEDIS)

  // Test Containers
  testImplementation(Libraries.TestContainers.JUNIT)
  testImplementation(Libraries.TestContainers.POSTGRESQL)
  testImplementation(Libraries.TestContainers.TEST_CONTAINERS)

  implementation("com.squareup.okhttp3:mockwebserver:4.9.2")
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
  from(tasks.javadoc)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = "core"

      artifact(sourcesJar.get())
      artifact(javadocJar.get())

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
            email.set("dev@c0x12c.com")
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
