import java.io.ByteArrayOutputStream

plugins {
  kotlin("jvm") version "1.9.23"
  kotlin("plugin.serialization") version "1.9.25"
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
  id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

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

configurations {
  testImplementation.get().extendsFrom(compileOnly.get())
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  api(project(":core"))

  kover(project(":core"))
}

allprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }
}

group = "com.c0x12c.featureflag"

fun getGitTag(): String {
  val stdout = ByteArrayOutputStream()
  exec {
    commandLine = "git describe --tags".split(" ")
    standardOutput = stdout
  }
  return stdout.toString().trim()
}

version = getGitTag()
println("Current version: $version")

subprojects {
  this.version = rootProject.version

  // Apply the Ktlint plugin to subprojects
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  // Configure Ktlint within subprojects
  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(false) // Set to true to see more detailed output
    verbose.set(true) // Display more information about linting
    android.set(false) // Set to true if you're using Android projects
    outputToConsole.set(true) // Display lint results in the console
    ignoreFailures.set(false) // Set to true to allow builds to pass even if there are lint errors
    enableExperimentalRules.set(false) // Enables experimental Ktlint rules

    filter {
      exclude("**/generated/**")
      include("**/src/main/kotlin/**")
      include("**/src/test/kotlin/**")
    }

    reporters {
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
      reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }
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
