package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.annotations.PlatformAnnotationDescriptor
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.versionSupport
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name

/** 已解析内置身份到静态描述符的投影；不扫描 PSI，也不把 custom 短名升级为 builtin。 */
public val CfirAnnotationCall.builtInDescriptor: BuiltInAnnotationDescriptor?
    get() {
        return when (val identity = annotationIdentity) {
            is CangjieAnnotationIdentity.LanguageBuiltIn ->
                BuiltInAnnotationRegistry.findLanguageBuiltIn(identity.sourceName)
                    ?.takeIf { it.kind == identity.kind }
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
        ?: when (annotationKind) {
            // JAVA is an official AST/metadata identity, not a source builtin
            // descriptor.  It still belongs to the Java interop feature gate.
            org.cangnova.cangjie.annotations.BuiltInAnnotationKind.JAVA ->
                LanguageFeature.JavaInteropAnnotations.versionSupport(settings)
            else -> null
        }

/**
 * 判断一个已解析 builtin identity 是否允许被当前 semantic owner 消费。
 *
 * Core C FFI kinds have no newer feature gate and therefore remain supported;
 * metadata-only kinds such as JAVA are routed through [annotationVersionSupport].
 */
public fun CfirAnnotationCall.isSupportedBuiltinAnnotation(
    kind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind,
    settings: LanguageVersionSettings,
): Boolean = annotationKind == kind &&
    (annotationVersionSupport(settings) ?: AnnotationVersionSupportStatus.SUPPORTED) ==
    AnnotationVersionSupportStatus.SUPPORTED

/** 完成注解解析后按真实参数名取得值。 */
public fun CfirAnnotation.argumentValue(name: String): CfirExpression? =
    argumentMapping.mapping[Name.identifier(name)]

/** 按参数名读取字符串字面量实参；参数不存在或值不是字符串时返回 `null`。 */
public fun CfirAnnotation.stringArgument(name: String): String? =
    (argumentValue(name) as? CfirLiteralExpression)?.value as? String

/** 按参数名读取布尔字面量实参；参数不存在或值不是布尔时返回 `null`。 */
public fun CfirAnnotation.booleanArgument(name: String): Boolean? =
    (argumentValue(name) as? CfirLiteralExpression)?.value as? Boolean

/** 统一规范化源代码内置身份；此函数只在类型/注解解析 owner 中调用。 */
public fun CfirAnnotationCall.resolveBuiltinAnnotationIdentity(): BuiltInAnnotationDescriptor? {
    val userType = typeRef as? org.cangnova.cangjie.cfir.types.CfirUserTypeRef
    val sourceName = annotationSourceName
        ?: userType?.qualifier?.joinToString(".") { it.name.asString() }
        ?: (calleeReference as? CfirNamedReference)?.name?.asString()
        ?: return null
    val languageDescriptor = BuiltInAnnotationRegistry.resolveLanguageBuiltIn(sourceName, forcedCustom, sourceModuleName)
    val packageDescriptor = if (languageDescriptor == null &&
        containingDeclarationSymbol is CfirFileSymbol &&
        containingDeclarationSymbol.cfir is CfirFile
    ) {
        BuiltInAnnotationRegistry.resolvePackageDirective(sourceName, forcedCustom)
    } else {
        null
    }
    val descriptor = languageDescriptor ?: packageDescriptor
    replaceAnnotationKind(descriptor?.kind)
    if (descriptor != null) {
        replaceAnnotationOrigin(
            if (languageDescriptor != null) {
                CangjieAnnotationOrigin.LANGUAGE_BUILT_IN
            } else {
                CangjieAnnotationOrigin.PACKAGE_DIRECTIVE
            },
        )
        replaceAnnotationIdentity(
            if (languageDescriptor != null) {
                CangjieAnnotationIdentity.LanguageBuiltIn(
                    requireNotNull(descriptor.kind),
                    descriptor.sourceName,
                )
            } else {
                CangjieAnnotationIdentity.PackageDirective(
                    org.cangnova.cangjie.annotations.CangjiePackageDirectiveKind.NON_PRODUCT,
                    descriptor.sourceName,
                )
            },
        )
        replaceAnnotationResolveState(CfirAnnotationResolveState.TYPE_RESOLVED)
    }
    return descriptor
}

/**
 * 仅在真实 annotation ClassId 已解析并命中 platform descriptor 后发布平台身份。
 *
 * 未解析类型、错误类型和同名 custom annotation 都保持 custom/unknown；源码短名
 * 只能作为显示信息，不能制造平台身份。
 */
public fun CfirAnnotationCall.resolvePlatformAnnotationIdentity(): Boolean {
    val resolvedClassFqName = annotationClassId
        ?.asSingleFqName()
        ?.takeUnless {
            typeRef is CfirErrorTypeRef ||
                typeRef.coneTypeOrNull?.isError == true ||
                it.asString() == "<error>" ||
                it.asString().contains("<error>")
        }
    if (forcedCustom) return false
    val descriptor = resolvedClassFqName?.let(BuiltInAnnotationRegistry::findPlatformAnnotation)
        ?: return false
    val classFqName = resolvedClassFqName ?: return false
    replaceAnnotationKind(null)
    replaceAnnotationOrigin(CangjieAnnotationOrigin.PLATFORM_DERIVED)
    replaceAnnotationIdentity(
        CangjieAnnotationIdentity.PlatformDerived(
            kind = descriptor.platformKind,
            classFqName = classFqName,
            sourceName = descriptor.sourceName,
        ),
    )
    replaceAnnotationResolveState(CfirAnnotationResolveState.TYPE_RESOLVED)
    return true
}
