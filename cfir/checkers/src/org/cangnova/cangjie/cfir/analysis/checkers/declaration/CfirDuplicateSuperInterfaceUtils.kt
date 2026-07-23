package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.ordinarySupertypeTypeOrNull
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import java.util.IdentityHashMap

/**
 * 重复父接口实例化 key 渲染的最大递归深度。
 */
private const val DUPLICATE_SUPER_INTERFACE_TYPE_KEY_MAX_DEPTH = 64

/**
 * 泛型父接口重复继承的共享遍历。
 *
 * 官方 `GetDupSuperInterface` 在每个声明层维护当前层已见接口集合，同时共享
 * `passedClassLikeDecls`，因此能区分“同一父接口实例内部重复”和“不同父接口分支交叉重复”。
 */
context(context: CheckerContext)
internal fun CfirClassLikeDeclaration.findInstantiatedDuplicateSuperInterface(
    substitutor: ConeSubstitutor,
    passedDeclarations: MutableSet<CfirClassLikeDeclaration>,
    checkExtendInterfaces: Boolean = false,
    instantiatedSelfType: ConeCangJieType? = null,
): Name? {
    if (!passedDeclarations.add(this)) return null

    val seenInCurrentDeclaration = linkedMapOf<String, Name>()
    for (superTypeRef in superTypeRefs) {
        val supertype = superTypeRef
            .classifyDeclaredSupertype(context.session)
            .ordinarySupertypeTypeOrNull()
            ?: continue
        collectInstantiatedSuperInterfaceInCurrentDeclaration(
            type = substitutor.substituteOrSelf(supertype),
            seen = seenInCurrentDeclaration,
            passedDeclarations = passedDeclarations,
        )?.let {
            return it
        }
    }
    if (checkExtendInterfaces && typeParameters.isNotEmpty() && instantiatedSelfType != null) {
        collectInstantiatedExtendInterfaceInCurrentDeclaration(
            instantiatedSelfType = instantiatedSelfType,
            seen = seenInCurrentDeclaration,
            passedDeclarations = passedDeclarations,
        )?.let {
            return it
        }
    }
    return null
}

/**
 * 收集当前声明直接父类型中的实例化接口，并检查当前层是否重复。
 */
context(context: CheckerContext)
private fun collectInstantiatedSuperInterfaceInCurrentDeclaration(
    type: ConeCangJieType,
    seen: MutableMap<String, Name>,
    passedDeclarations: MutableSet<CfirClassLikeDeclaration>,
): Name? {
    val classifierType = type as? ConeClassifierType ?: return null
    val symbol = classifierType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
    val declaration = symbol.cfir as? CfirInterface ?: return null
    val typeKey = classifierType.instantiatedInterfaceKey() ?: return null
    seen.putIfAbsent(typeKey, declaration.name)?.let { return it }

    val nestedSubstitutor = declaration.createDeclarationTypeSubstitutor(classifierType)
    return declaration.findInstantiatedDuplicateSuperInterface(nestedSubstitutor, passedDeclarations)
}

/**
 * 收集当前声明可访问 extend 引入的实例化接口，并检查当前层是否重复。
 */
context(context: CheckerContext)
private fun collectInstantiatedExtendInterfaceInCurrentDeclaration(
    instantiatedSelfType: ConeCangJieType,
    seen: MutableMap<String, Name>,
    passedDeclarations: MutableSet<CfirClassLikeDeclaration>,
): Name? {
    val targetKey = instantiatedSelfType.expandedExtendTargetKey ?: return null
    val extendProvider = context.session.extendProvider
    for (extend in extendProvider.getExtendsForTarget(targetKey)) {
        if (!extendProvider.isExtendAccessible(extend)) continue
        val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: continue
        val substitution = createExtendDeclarationSubstitution(
            session = context.session,
            extend = extend,
            targetPattern = targetPattern,
            concreteReceiverType = instantiatedSelfType,
        ) ?: continue

        collectInstantiatedExtendInterfaceInCurrentDeclaration(
            extend = extend,
            substitutor = substitution.substitutor,
            seen = seen,
            passedDeclarations = passedDeclarations,
        )?.let {
            return it
        }
    }
    return null
}

/**
 * 收集单个 extend 声明 super type 中的实例化接口。
 */
context(context: CheckerContext)
private fun collectInstantiatedExtendInterfaceInCurrentDeclaration(
    extend: CfirExtend,
    substitutor: ConeSubstitutor,
    seen: MutableMap<String, Name>,
    passedDeclarations: MutableSet<CfirClassLikeDeclaration>,
): Name? {
    for (superTypeRef in extend.superTypeRefs) {
        val supertype = superTypeRef.coneTypeOrNull ?: continue
        collectInstantiatedSuperInterfaceInCurrentDeclaration(
            type = substitutor.substituteOrSelf(supertype),
            seen = seen,
            passedDeclarations = passedDeclarations,
        )?.let {
            return it
        }
    }
    return null
}

/**
 * 渲染 classifier 类型的实例化接口 key。
 *
 * key 同时包含接口 ClassId 和类型实参，用于区分 `I<Int>` 与 `I<String>`。
 */
context(context: CheckerContext)
internal fun ConeClassifierType.instantiatedInterfaceKey(): String? {
    val symbol = toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
    return buildString {
        append(symbol.classId.asString())
        if (typeArguments.isNotEmpty()) {
            append('<')
            typeArguments.joinTo(this) { it.type.renderForInstantiationDiagnostic() }
            append('>')
        }
    }
}

/**
 * 根据具体类型实参为声明创建类型参数替换器。
 */
context(context: CheckerContext)
internal fun CfirTypeParameterRefsOwner.createDeclarationTypeSubstitutor(
    type: ConeCangJieType,
): ConeSubstitutor {
    val lookupType = type as? ConeLookupTagBasedType ?: return ConeSubstitutor.Empty
    if (typeParameters.isEmpty() || typeParameters.size != lookupType.typeArguments.size) {
        return ConeSubstitutor.Empty
    }
    val substitutions = typeParameters.zip(lookupType.typeArguments).associate { (typeParameter, argument) ->
        typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument.type
    }
    return createTypeSubstitutorByTypeConstructor(
        map = substitutions,
        context = context.session.typeContext,
        approximateIntegerLiterals = false,
    )
}

/**
 * 将类型渲染为重复实例化接口诊断 key 的稳定文本。
 */
private fun ConeCangJieType.renderForInstantiationDiagnostic(
    visited: MutableSet<ConeCangJieType> = java.util.Collections.newSetFromMap(IdentityHashMap()),
    depth: Int = 0,
): String {
    if (depth >= DUPLICATE_SUPER_INTERFACE_TYPE_KEY_MAX_DEPTH) return "..."
    if (!visited.add(this)) return "..."
    return when (this) {
        is ConeTypeParameterType -> "P@${System.identityHashCode(lookupTag.typeParameterSymbol)}"

        is ConeLookupTagBasedType -> buildString {
            append(lookupTag.name.asString())
            if (typeArguments.isNotEmpty()) {
                append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                    it.type.renderForInstantiationDiagnostic(visited, depth + 1)
                })
            }
        }

        is ConeTupleType -> elementTypes.joinToString(prefix = "(", postfix = ")") {
            it.renderForInstantiationDiagnostic(visited, depth + 1)
        }
        is ConeFunctionType -> buildString {
            append(parameterTypes.joinToString(prefix = "(", postfix = ")") {
                it.renderForInstantiationDiagnostic(visited, depth + 1)
            })
            append(" -> ")
            append(returnType.renderForInstantiationDiagnostic(visited, depth + 1))
        }
        is ConeVArrayType -> "VArray<${elementType.renderForInstantiationDiagnostic(visited, depth + 1)}, $size>"
        is ConePointerType -> "CPointer<${pointeeType.renderForInstantiationDiagnostic(visited, depth + 1)}>"
        is ConeIntersectionType -> intersectedTypes.joinToString(prefix = "(", postfix = ")", separator = " & ") {
            it.renderForInstantiationDiagnostic(visited, depth + 1)
        }
        is ConeUnionType -> unionTypes.joinToString(prefix = "(", postfix = ")", separator = " | ") {
            it.renderForInstantiationDiagnostic(visited, depth + 1)
        }
        is ConePrimitiveType -> kind.typeName
        is ConeCStringType -> "CString"
        is ConeQuestType -> "?"
        else -> javaClass.simpleName
    }
}
