package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings

/**
 * Result of applying the language/API version gate to an annotation contract.
 *
 * The parser must retain newer syntax for IDE and cross-version analysis.  The
 * semantic owner consumes this result and turns [UNSUPPORTED_LANGUAGE_VERSION]
 * into the compiler diagnostic; it must not reinterpret the annotation as a
 * custom annotation.
 */
public enum class AnnotationVersionSupportStatus {
    SUPPORTED,
    UNSUPPORTED_LANGUAGE_VERSION,
    UNSUPPORTED_API_VERSION,
}

/** Version gate for language built-ins and system annotations. */
public fun AnnotationDescriptor.versionSupport(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus =
    requiredLanguageFeature?.versionSupport(settings) ?: AnnotationVersionSupportStatus.SUPPORTED

/** Version gate for platform annotations resolved by their real class identity. */
public fun PlatformAnnotationDescriptor.versionSupport(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus =
    requiredLanguageFeature?.versionSupport(settings) ?: AnnotationVersionSupportStatus.SUPPORTED

/**
 * 为只存在于官方 AST/二进制 metadata 中的互操作 kind 提供同一版本门禁。
 *
 * `JAVA` 不是 v1.0.0 源码 parser 的 builtin spelling，但它仍是官方 AST 中
 * 的稳定身份。它和平台注解一样必须在 semantic consumer 读取前经过
 * [LanguageVersionSettings]，不能因为没有 source descriptor 就绕过版本策略。
 */
public fun LanguageFeature.versionSupport(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus = versionSupportNullable(settings)

/**
 * Apply both the language and API introduced-version constraints.
 *
 * This helper intentionally uses [LanguageVersionSettings.supportsFeature] so
 * explicit feature overrides remain authoritative and no caller can silently
 * introduce a raw version comparison.
 */
private fun LanguageFeature?.versionSupportNullable(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus {
    this ?: return AnnotationVersionSupportStatus.SUPPORTED
    // Keep explicit settings authoritative, exactly as LanguageVersionSettings
    // does for every other consumer.  An explicit disable is a semantic feature
    // decision, not an API-version failure; an explicit enable may intentionally
    // exercise a newer feature in a compatibility/test session.
    settings.getCustomizedLanguageFeatures()[this]?.let { state ->
        return if (state == LanguageFeature.State.ENABLED) {
            AnnotationVersionSupportStatus.SUPPORTED
        } else {
            AnnotationVersionSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
        }
    }

    if (this.sinceVersion != null && settings.languageVersion < this.sinceVersion) {
        return AnnotationVersionSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
    }
    if (settings.apiVersion < this.sinceApiVersion) {
        return AnnotationVersionSupportStatus.UNSUPPORTED_API_VERSION
    }
    return if (settings.supportsFeature(this)) {
        AnnotationVersionSupportStatus.SUPPORTED
    } else {
        AnnotationVersionSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
    }
}
