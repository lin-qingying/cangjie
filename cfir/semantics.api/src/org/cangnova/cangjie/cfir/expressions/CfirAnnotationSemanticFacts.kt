package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.annotations.*

/** 已解析内置身份到静态描述符的投影；不扫描 PSI，也不把 custom 短名升级为 builtin。 */
public val CfirAnnotationCall.builtInDescriptor: BuiltInAnnotationDescriptor?
    get() {
        return when (val identity = annotationIdentity) {
            is CangjieAnnotationIdentity.LanguageBuiltIn ->
                BuiltInAnnotationRegistry.findLanguageBuiltIn(identity.kind, identity.sourceName)
            is CangjieAnnotationIdentity.PackageDirective ->
                BuiltInAnnotationRegistry.findPackageDirective(identity.sourceName)
                    ?.takeIf { it.origin == CangjieAnnotationOrigin.PACKAGE_DIRECTIVE }
            else -> null
        }
    }

/**
 * 已由类型解析确认的互操作库注解描述符。
 *
 * 平台注解不写入 [CfirAnnotationCall.annotationKind]；这里的命中必须来自真实
 * annotation ClassId，防止同名 custom annotation 触发 Java/ObjC 语义。
 */
public val CfirAnnotationCall.platformAnnotationDescriptor: PlatformAnnotationDescriptor?
    get() {
        val identity = annotationIdentity as? CangjieAnnotationIdentity.PlatformDerived ?: return null
        return BuiltInAnnotationRegistry.findPlatformAnnotation(identity.classFqName)
            ?.takeIf { it.platformKind == identity.kind }
    }

/** 平台注解的已解析独立身份，供 CFIR semantic owner 消费。 */
public val CfirAnnotationCall.platformAnnotationKind: CangjiePlatformAnnotationKind?
    get() = platformAnnotationDescriptor?.platformKind

/**
 * 统一的 annotation language-feature gate。
 *
 * TYPES/identity 只发布 descriptor；所有声明 checker 和 interop provider
 * 从这里读取同一版本状态，避免每个 consumer 自己重新组合 builtin/platform
 * 分支或直接比较 LanguageVersion/API 版本。
 */
public fun CfirAnnotationCall.annotationVersionSupport(
    settings: LanguageVersionSettings,
): AnnotationVersionSupportStatus? =
    builtInDescriptor?.versionSupport(settings)
        ?: platformAnnotationDescriptor?.versionSupport(settings)
        ?: (annotationIdentity as? CangjieAnnotationIdentity.SystemMacro)
            ?.classFqName
            ?.let(BuiltInAnnotationRegistry::findSystemAnnotation)
            ?.versionSupport(settings)
        ?: annotationKind?.requiredLanguageFeature?.versionSupport(settings)

/**
 * 判断一个已解析 builtin identity 是否允许被当前 semantic owner 消费。
 *
 * The common kind gate is authoritative for both the generic builtin catalog
 * and kind-specific version policy; annotation resolve status is then checked
 * for the diagnostic reason and resolved identity.
 */
public fun CfirAnnotationCall.isSupportedBuiltinAnnotation(
    kind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind,
    settings: LanguageVersionSettings,
): Boolean = annotationKind == kind &&
    settings.supportsBuiltinAnnotationKind(kind) &&
    (annotationVersionSupport(settings) ?: AnnotationVersionSupportStatus.SUPPORTED) ==
    AnnotationVersionSupportStatus.SUPPORTED
