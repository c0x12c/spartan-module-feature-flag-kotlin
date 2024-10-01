package utils

import com.goncalossilva.murmurhash.MurmurHash3
import kotlin.math.absoluteValue

class PercentageMatchingUtil {
  companion object {
    fun isTargetedBasedOnPercentage(
      value: String,
      percentage: Double
    ): Boolean {
      val hash = Companion.murmur128x64(value).first.absoluteValue
      val hashPercentage = (hash % 100) + 1
      return hashPercentage <= percentage
    }

    /**
     * Calculates MurmurHash for a string.
     */
    fun murmur128x64(value: String): Pair<Long, Long> {
      val hash = MurmurHash3().hash128x64(value.encodeToByteArray())
      return Pair(hash[0].toLong(), hash[1].toLong())
    }
  }
}
