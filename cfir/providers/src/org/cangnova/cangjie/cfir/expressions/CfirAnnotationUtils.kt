package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.name.Name

/** 已解析内置身份到静态描述符的投影；不扫描 PSI，也不把 custom 短名升级为 builtin。 */
public val CfirAnnotationCall.builtInDescriptor: BuiltInAnnotationDescriptor?
    get() {
        val identity = annotationIdentity as? CangjieAnnotationIdentity.LanguageBuiltIn ?: return null
        return BuiltInAnnotationRegistry.findLanguageBuiltIn(identity.sourceName)
            ?.takeIf { it.kind == identity.kind }
    }

/** 完成注解解析后按真实参数名取得值。 */
public fun CfirAnnotation.argumentValue(name: String): CfirExpression? =
    argumentMapping.mapping[Name.identifier(name)]

public fun CfirAnnotation.stringArgument(name: String): String? =
    (argumentValue(name) as? CfirLiteralExpression)?.value as? String

public fun CfirAnnotation.booleanArgument(name: String): Boolean? =
    (argumentValue(name) as? CfirLiteralExpression)?.value as? Boolean

/** 统一规范化源代码内置身份；此函数只在类型/注解解析 owner 中调用。 */
public fun CfirAnnotationCall.resolveBuiltinAnnotationIdentity(): BuiltInAnnotationDescriptor? {
    val userType = typeRef as? org.cangnova.cangjie.cfir.types.CfirUserTypeRef
    val sourceName = annotationSourceName
        ?: userType?.qualifier?.joinToString(".") { it.name.asString() }
        ?: (calleeReference as? CfirNamedReference)?.name?.asString()
        ?: return null
    val descriptor = BuiltInAnnotationRegistry.resolveLanguageBuiltIn(sourceName, forcedCustom, sourceModuleName)
    replaceAnnotationKind(descriptor?.kind)
    if (descriptor != null) {
        replaceAnnotationOrigin(CangjieAnnotationOrigin.LANGUAGE_BUILT_IN)
        replaceAnnotationIdentity(CangjieAnnotationIdentity.LanguageBuiltIn(descriptor.kind, descriptor.sourceName))
        replaceAnnotationResolveState(CfirAnnotationResolveState.TYPE_RESOLVED)
    }
    return descriptor
}
