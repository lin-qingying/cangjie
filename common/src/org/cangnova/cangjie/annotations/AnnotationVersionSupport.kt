package org.cangnova.cangjie.annotations

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageFeatureSupportStatus
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.featureSupportStatus

/**
 * Result of applying the language version gate to an annotation contract.
 *
 * The parser must retain newer syntax for IDE and cross-version analysis.  The
 * semantic owner consumes this result and turns [UNSUPPORTED_LANGUAGE_VERSION]
 * into the compiler diagnostic; it must not reinterpret the annotation as a
 * custom annotation.
 */
public enum class AnnotationVersionSupportStatus {
    SUPPORTED,
    UNSUPPORTED_LANGUAGE_VERSION,
    DISABLED,
    EXPERIMENTAL,
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
 * Apply the language introduced-version constraint.
 *
 * This helper delegates to the common [LanguageVersionSettings.featureSupportStatus]
 * owner so explicit feature overrides remain authoritative and no caller can
 * introduce a second raw version comparison.
 */
private fun LanguageFeature?.versionSupportNullable(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus {
    this ?: return AnnotationVersionSupportStatus.SUPPORTED
    return when (settings.featureSupportStatus(this)) {
        LanguageFeatureSupportStatus.SUPPORTED -> AnnotationVersionSupportStatus.SUPPORTED
        LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION ->
            AnnotationVersionSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
        LanguageFeatureSupportStatus.DISABLED -> AnnotationVersionSupportStatus.DISABLED
        LanguageFeatureSupportStatus.EXPERIMENTAL -> AnnotationVersionSupportStatus.EXPERIMENTAL
    }
}
