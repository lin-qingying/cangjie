package org.cangnova.cangjie.cfir.resolve.match

import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/** ChkTypePattern 的共享结论；不依赖主语是否拥有已知运行时值。 */
fun resolveTypePatternMatchingKind(
    subjectType: ConeCangJieType?,
    targetType: ConeCangJieType?,
    session: CfirSession,
): CfirTypePatternMatchingKind {
    val subject = subjectType?.fullyExpandedType(session) ?: return CfirTypePatternMatchingKind.UNKNOWN
    val target = targetType?.fullyExpandedType(session) ?: return CfirTypePatternMatchingKind.UNKNOWN
    if (subject.containsErrorType() || target.containsErrorType()) return CfirTypePatternMatchingKind.UNKNOWN
    return when {
        subject.isTypePatternOrdinarySubtypeOf(target, session) -> CfirTypePatternMatchingKind.ALWAYS
        subject.needsRuntimeTypeCheckAgainst(target, session) -> CfirTypePatternMatchingKind.RUNTIME
        else -> CfirTypePatternMatchingKind.NEVER
    }
}

/** 对齐官方 IsNeedRuntimeCheck，正反子类型与泛型形态都保留运行期检查机会。 */
fun ConeCangJieType.needsRuntimeTypeCheckAgainst(targetType: ConeCangJieType, session: CfirSession): Boolean {
    val source = fullyExpandedType(session)
    val target = targetType.fullyExpandedType(session)
    if (source.isFinalForRuntimeTypeCheck(session) && target.isFinalForRuntimeTypeCheck(session)) {
        val sourceDeclaration = source.runtimeDeclarationClassIdOrNull()
        val targetDeclaration = target.runtimeDeclarationClassIdOrNull()
        if (sourceDeclaration != null || targetDeclaration != null) return sourceDeclaration == targetDeclaration
    }
    return source is ConeClassifierType && target is ConeClassifierType ||
            source.hasRuntimeGenericShape() || target.hasRuntimeGenericShape() ||
            source.isTypePatternOrdinarySubtypeOf(target, session) ||
            target.isTypePatternOrdinarySubtypeOf(source, session)
}

/** 官方 final 集合不包括 interface；pointer/CString/数值和值类型属于 final。 */
private fun ConeCangJieType.isFinalForRuntimeTypeCheck(session: CfirSession): Boolean = when (this) {
    is ConePrimitiveType, is ConeStructType, is ConeEnumType, is ConeVArrayType,
    is ConePointerType, is ConeCStringType -> true
    is ConeClassLikeType -> {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)
        if (symbol == null || !symbol.isBound) false else {
            symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
            val declaration = symbol.cfir
            declaration !is CfirInterface && declaration is CfirMemberDeclaration &&
                    !declaration.status.isAbstract && !declaration.status.isOpen
        }
    }
    else -> false
}

/** 没有名义声明的 primitive/tuple/function/pointer 不制造声明身份。 */
private fun ConeCangJieType.runtimeDeclarationClassIdOrNull(): ClassId? = when (this) {
    is ConeClassLikeType -> classId
    is ConeStructType -> classId
    is ConeEnumType -> classId
    else -> null
}

/** 类型自身或其结构分量中的泛型保留运行时可能性。 */
private fun ConeCangJieType.hasRuntimeGenericShape(): Boolean = when (this) {
    is ConeTypeParameterType, is ConeTypeVariableType, is ConeStubType -> true
    is ConeTupleType -> elementTypes.any { it.hasRuntimeGenericShape() }
    is ConeFunctionType -> parameterTypes.any { it.hasRuntimeGenericShape() } || returnType.hasRuntimeGenericShape()
    is ConePointerType -> pointeeType.hasRuntimeGenericShape()
    is ConeVArrayType -> elementType.hasRuntimeGenericShape()
    else -> typeArguments.any { it.type.hasRuntimeGenericShape() }
}
