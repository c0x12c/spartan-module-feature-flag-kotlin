package com.c0x12c.featureflag.dependency

object Libraries {
  object Exposed {
    const val CORE = "org.jetbrains.exposed:exposed-core:${Versions.EXPOSED}"
    const val DAO = "org.jetbrains.exposed:exposed-dao:${Versions.EXPOSED}"
    const val JAVA_TIME = "org.jetbrains.exposed:exposed-java-time:${Versions.EXPOSED}"
    const val JDBC = "org.jetbrains.exposed:exposed-jdbc:${Versions.EXPOSED}"
  }

  object Goncalossilva {
    const val MURMURHASH = "com.goncalossilva:murmurhash:0.4.0"
  }

  object Jackson {
    const val KOTLIN = "com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2"
  }

  object Kotlinx {
    const val KOTLINX_COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0"
    const val KOTLINX_COROUTINES_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
    const val KOTLINX_DATETIME = "org.jetbrains.kotlinx:kotlinx-datetime:0.6.1"
    const val KOTLINX_SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1"
  }

  object Logging {
    const val LOGBACK_CLASSIC = "ch.qos.logback:logback-classic:1.5.8"
    const val SLF4J = "org.slf4j:slf4j-api:2.0.6"
  }

  object JavaxInject {
    const val INJECT = "javax.inject:javax.inject:1"
  }

  object Junit5 {
    const val API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT5}"
    const val ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT5}"
  }

  object Mockk {
    const val MOCKK = "io.mockk:mockk:1.13.12"
  }

  object Maven {
    const val ARTIFACT = "org.apache.maven:maven-artifact:3.9.9"
  }

  object PostgreSQL {
    const val POSTGRESQL = "org.postgresql:postgresql:42.7.4"
  }

  object Redis {
    const val JEDIS = "redis.clients:jedis:5.1.5"
  }

  object Retrofit2 {
    const val CONVERTER_GSON = "com.squareup.retrofit2:converter-gson:${Versions.RETROFIT2}"
    const val RETROFIT = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT2}"
  }

  object TestContainers {
    const val JUNIT = "org.testcontainers:junit-jupiter:${Versions.TEST_CONTAINERS}"
    const val POSTGRESQL = "org.testcontainers:postgresql:${Versions.TEST_CONTAINERS}"
    const val TEST_CONTAINERS = "org.testcontainers:testcontainers:${Versions.TEST_CONTAINERS}"
  }

  object Zaxxer {
    const val HIKARI = "com.zaxxer:HikariCP:6.0.0"
  }
}
