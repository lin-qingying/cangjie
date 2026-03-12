package org.cangjie.config

data class LanguageVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<LanguageVersion> {
    override fun compareTo(other: LanguageVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): LanguageVersion? {
            val parts = value.split('.')
            if (parts.size != 3) return null

            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return LanguageVersion(major, minor, patch)
        }
    }
}

object LanguageVersions {
    val V_1_0_5: LanguageVersion = LanguageVersion(1, 0, 5)
    val LATEST_STABLE: LanguageVersion = V_1_0_5
}

enum class LanguageFeature {
    ;

    companion object {
        fun fromName(name: String): LanguageFeature? {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
    }
}

enum class WarningLevel {
    Error,
    Warning,
    Disabled,
}

class AnalysisFlag<T>(val defaultValue: T)

object AnalysisFlags {
    val warningLevels = AnalysisFlag<Map<String, WarningLevel>>(emptyMap())
}

data class LanguageVersionSettings(
    val languageVersion: LanguageVersion = LanguageVersions.LATEST_STABLE,
    val enabledFeatures: Set<LanguageFeature> = emptySet(),
    val analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
) {
    fun supportsFeature(feature: LanguageFeature): Boolean = feature in enabledFeatures

    @Suppress("UNCHECKED_CAST")
    fun <T> getFlag(flag: AnalysisFlag<T>): T {
        return analysisFlags[flag] as? T ?: flag.defaultValue
    }

    companion object {
        val DEFAULT: LanguageVersionSettings = LanguageVersionSettings()
    }
}
