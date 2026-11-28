package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeSources

/** 已准备父 scope 的存储类型为函数权威值，非函数使用该 scope 已代换的 header。 */
internal fun captureInheritanceScopeMembers(scope: CfirTypeScope, type: ConeCangJieType, sourceExtend: CfirExtend?,
    interfaceMember: Boolean, sources: CfirTypeAwareSupertypeSources,
    aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?): List<CfirMappedInheritedMember> {
    fun normalize(type: ConeCangJieType) = type.fullyExpandedTypeUsingAbbreviation(sources.session, sources.symbols, aliasExpansion)
    val result = ArrayList<CfirMappedInheritedMember>()
    for (name in scope.getCallableNames()) {
        val table = (scope as? CfirFunctionInstantiationScope)?.publishedFunctionMemberTable(name)
        table?.members?.forEach { input ->
            val stored = input.copy(parameterTypes = input.parameterTypes.map(::normalize), returnType = normalize(input.returnType),
                upperBounds = input.upperBounds.mapValues { (_, bounds) -> bounds.map(::normalize) },
                inconsistentReturnTypes = input.inconsistentReturnTypes.map(::normalize), returnUpperBounds = input.returnUpperBounds.map(::normalize))
                .freezeFunctionMember()
            val declaration = stored.declaration.cfir as? CfirFunction ?: return@forEach
            val status = declaration.status
            val actualOwner = checkNotNull(stored.declaration.getContainingExtend() ?: stored.declaration.getContainingClass()?.cfir) {
                "A prepared inherited function requires its actual declaration owner"
            }
            val projection = (scope as? CfirCjmpMemberSubstitutorProvider)?.cjmpMemberOwnerFor(stored.declaration)
            check(projection == null || projection.physicalOwner === actualOwner) { "The prepared owner projection must retain the original physical declaration" }
            result += CfirMappedInheritedMember(stored.declaration, name, CfirInheritanceMemberKind.FUNCTION,
                actualOwner, projection?.logicalOwner ?: actualOwner, stored.structType, stored.baseScope,
                CfirInheritanceMemberFlags(status.isStatic, status.isOverride, status.isRedef, status.isConst, status.isMut,
                    status.isDefault, status.isAbstract, status.visibility, false, false, false, status.isOpen, stored.declaration.getContainingClass()?.cfir is CfirInterface, stored.isConstructor, status.isOperator),
                stored.returnType, stored.parameterTypes, declaration.valueParameters.map { it.name },
                declaration.valueParameters.map { it.isNamed }, stored.upperBounds.keys.toList(), stored.upperBounds,
                stored.inconsistentReturnTypes, stored, stored.replaceOther, interfaceMember, stored.shouldBeImplemented, sourceExtend,
                stored.returnUpperBounds)
        }
        scope.processCallablesByName(name) { symbol ->
            val declaration = symbol.cfir
            if (declaration is CfirFunction) return@processCallablesByName
            val callable = declaration as? org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ?: return@processCallablesByName
            val kind = when (declaration) { is CfirProperty -> CfirInheritanceMemberKind.PROPERTY
                is CfirFieldVariable -> CfirInheritanceMemberKind.VARIABLE
                else -> return@processCallablesByName }
            val returnType = callable.returnTypeRef.coneTypeOrNull?.let(::normalize) ?: return@processCallablesByName
            val status = callable.status
            val actualOwner = checkNotNull(symbol.getContainingExtend() ?: symbol.getContainingClass()?.cfir) {
                "A prepared inherited member requires its actual declaration owner"
            }
            val projection = (scope as? CfirCjmpMemberSubstitutorProvider)?.cjmpMemberOwnerFor(symbol)
            check(projection == null || projection.physicalOwner === actualOwner) { "The prepared owner projection must retain the original physical declaration" }
            result += CfirMappedInheritedMember(symbol, name, kind, actualOwner, projection?.logicalOwner ?: actualOwner, type, scope,
                CfirInheritanceMemberFlags(status.isStatic, status.isOverride, status.isRedef, status.isConst, status.isMut,
                    status.isDefault, status.isAbstract, status.visibility, (declaration as? CfirFieldVariable)?.isVar ?: status.isMut,
                    (declaration as? CfirProperty)?.getter != null, (declaration as? CfirProperty)?.setter != null,
                status.isOpen, symbol.getContainingClass()?.cfir is CfirInterface, false, status.isOperator),
                returnType, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), emptyList(), null,
                false, interfaceMember, false, sourceExtend, emptyList())
        }
    }
    return java.util.Collections.unmodifiableList(result)
}
