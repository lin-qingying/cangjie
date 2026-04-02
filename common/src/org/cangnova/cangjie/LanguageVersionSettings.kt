package org.cangnova.cangjie

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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
    /**
     * Enables data-flow-analysis based warnings in frontend diagnostics.
     */
    EnableDfaWarnings,
    /**
     * Reports lambda/function value mismatch as ARGUMENT_TYPE_MISMATCH instead of
     * RETURN_TYPE_MISMATCH on lambda body return expression.
     */
    LambdaReturnTypeMismatchAsArgumentTypeMismatch,
    InvalidBinaryOperatorDiagnostics,
    LexicographicVariableReadinessCalculation,
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

class AnalysisFlag<out T> internal constructor(
    private val name: String,
    val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    object Delegates {
        open class Boolean(val defaultValue: kotlin.Boolean) {
            companion object : Boolean(defaultValue = false)

            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, defaultValue)
        }
        object WarningLevelMap {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>):  AnalysisFlag.Delegate<Map<String, WarningLevel>> = Delegate(property.name, emptyMap())
        }





    }
}

object AnalysisFlags {
    val warningLevels by AnalysisFlag.Delegates.WarningLevelMap

    @JvmStatic
    val ideMode by AnalysisFlag.Delegates.Boolean
    @JvmStatic
    val stdlibCompilation by AnalysisFlag.Delegates.Boolean

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
