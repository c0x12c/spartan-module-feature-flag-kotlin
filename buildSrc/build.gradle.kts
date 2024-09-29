plugins {
  kotlin("jvm") version "1.9.24"
  kotlin("plugin.serialization") version "1.9.24"

  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
}
