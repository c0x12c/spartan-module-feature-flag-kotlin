plugins {
    kotlin("jvm") version "1.8.10"

    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "com.c0x12c"
version = "0.1.0"

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    mavenCentral()
}

ktlint {
    android.set(false) // Set to true if it's an Android project
    outputToConsole.set(true)
    ignoreFailures.set(false)

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    // Ensure it applies to Kotlin files in the correct directories
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.micronaut:micronaut-runtime:4.6.5")
    implementation("io.lettuce:lettuce-core:6.4.0.RELEASE")

    testImplementation(kotlin("test"))

    testImplementation("org.postgresql:postgresql:42.3.1")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")

    // MockK for mocking objects
    testImplementation("io.mockk:mockk:1.12.0")

    // Coroutines for suspend functions
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")

    testImplementation("org.testcontainers:testcontainers:1.17.3")
    testImplementation("org.testcontainers:postgresql:1.17.3")
    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "feature-flag-module"
        }
    }

    repositories {
        maven {
            url = uri("https://repo.yourpublishingserver.com/releases") // Replace with actual repo
        }
    }
}
