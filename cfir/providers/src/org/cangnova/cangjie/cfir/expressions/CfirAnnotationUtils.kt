package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.annotations.PlatformAnnotationDescriptor
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.requiredLanguageFeature
import org.cangnova.cangjie.annotations.supportsBuiltinAnnotationKind
import org.cangnova.cangjie.annotations.versionSupport
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name


/** 完成注解解析后按真实参数名取得值。 */
public fun CfirAnnotation.argumentValue(name: String): CfirExpression? =
    argumentMapping.mapping[Name.identifier(name)]
        ?: (this as? CfirAnnotationCall)?.argumentView?.entries
            ?.firstOrNull {
                !it.isDefaultOrigin &&
                    it.status == CfirAnnotationArgumentStatus.RESOLVED &&
                    it.resolvedParameter?.name == Name.identifier(name) &&
                    it.argument != null
            }
            ?.argument
            ?.let { (it as? CfirNamedArgumentExpression)?.expression ?: it }

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
