package com.c0x12c.featureflag.utils

import java.io.File
import java.io.FileNotFoundException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class VersionUtil {
  companion object {
    fun getVersionFromManifest(manifestFile: File): String {
      if (manifestFile.exists()) {
        val jsonString = manifestFile.readText()
        val manifest = Json.decodeFromString<JsonObject>(jsonString)
        return manifest.getValue("version").toString()
      } else {
        throw FileNotFoundException("Manifest file not found")
      }
    }
  }
}
