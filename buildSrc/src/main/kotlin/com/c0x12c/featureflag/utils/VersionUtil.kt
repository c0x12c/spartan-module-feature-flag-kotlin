package com.c0x12c.featureflag.utils

import com.c0x12c.featureflag.model.Manifest
import java.io.File
import java.io.FileNotFoundException
import kotlinx.serialization.json.Json

class VersionUtil {
  companion object {
    fun getVersionFromManifest(manifestFile: File): String {
      if (manifestFile.exists()) {
        val jsonString = manifestFile.readText()
        val manifest = Json.decodeFromString<Manifest>(jsonString)
        return manifest.version
      } else {
        throw FileNotFoundException("Manifest file not found")
      }
    }
  }
}
