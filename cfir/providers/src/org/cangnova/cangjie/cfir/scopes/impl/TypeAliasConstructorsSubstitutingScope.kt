package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructorCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.types.withAbbreviation

private object TypeAliasConstructorInfoKey : CfirDeclarationDataKey()

data class TypeAliasConstructorInfo<T : CfirFunction>(
    val originalConstructor: T,
    val typeAliasSymbol: CfirTypeAliasSymbol,
    val substitutor: ConeSubstitutor?,
)

var <T : CfirFunction> T.typeAliasConstructorInfo: TypeAliasConstructorInfo<T>? by CfirDeclarationDataRegistry.data(TypeAliasConstructorInfoKey)

val CfirConstructorSymbol.typeAliasConstructorInfo: TypeAliasConstructorInfo<*>?
    get() = cfir.typeAliasConstructorInfo

/**
 * typealias 构造器替换 scope。
 *
 * 对齐 Kotlin FIR `TypeAliasConstructorsSubstitutingScope`：当构造调用命中
 * typealias 名称时，本 scope 从展开类型的构造器 scope 中取真实构造器，再合成
 * 指向 typealias source、携带 typealias 类型参数的构造器候选。这样调用解析、
 * 约束系统和诊断都以 typealias 使用点为入口，同时语义仍落到展开类构造器。
 */
class TypeAliasConstructorsSubstitutingScope private constructor(
    private val typeAliasSymbol: CfirTypeAliasSymbol,
    private val delegatingScope: CfirScope,
    private val session: CfirSession,
) : CfirScope() {
    companion object {
        fun initialize(
            typeAliasSymbol: CfirTypeAliasSymbol,
            session: CfirSession,
            scopeSession: ScopeSession,
        ): CfirScope {
            val typeAlias = typeAliasSymbol.cfir
            val expandedType = typeAlias.expandedTypeRef.coneTypeOrNull
                ?.fullyExpandedType(session) as? ConeLookupTagBasedType
                ?: return CfirTypeScope.Empty
            val expandedClassId = expandedType.classIdOrPrimitiveClassId ?: return CfirTypeScope.Empty
            val expandedClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(expandedClassId)
                ?: return CfirTypeScope.Empty

            val expandedDeclaredScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = expandedClassSymbol,
                symbolProvider = session.symbolProvider,
                extendProvider = session.extendProviderOrNull,
                directSupertypeProvider = session.directSupertypeProviderOrNull,
                ownerType = expandedType,
                dispatchReceiverType = expandedType,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
            )
            val expandedTypeScope = CfirClassSubstitutionScope(session, expandedDeclaredScope, expandedType)
            return TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, expandedTypeScope, session)
        }
    }

    private val constructorCache = mutableMapOf<CfirConstructorSymbol, CfirConstructorSymbol>()

    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors { originalConstructorSymbol ->
            processor(constructorCache.getOrPut(originalConstructorSymbol) {
                createTypealiasConstructor(originalConstructorSymbol)
            })
        }
    }

    private fun createTypealiasConstructor(originalConstructorSymbol: CfirConstructorSymbol): CfirConstructorSymbol {
        val typeAlias = typeAliasSymbol.cfir
        val originalConstructor = originalConstructorSymbol.cfir
        val newConstructorSymbol = CfirConstructorSymbol(originalConstructorSymbol.callableId)

        val typealiasConstructor = buildConstructorCopy(originalConstructor) {
            source = typeAlias.source
            moduleData = typeAlias.moduleData
            origin = CfirDeclarationOrigin.Synthetic.TypeAliasConstructor
            symbol = newConstructorSymbol
            dispatchReceiverType = null
            returnTypeRef = originalConstructor.returnTypeRef.withTypeAliasAbbreviation(typeAliasSymbol)
            typeParameters.clear()
            typeParameters += typeAlias.typeParameters
            valueParameters.clear()
            valueParameters += originalConstructor.valueParameters.map { valueParameter ->
                buildValueParameterCopy(valueParameter) {
                    symbol = CfirValueParameterSymbol(valueParameter.symbol.callableId)
                    moduleData = typeAlias.moduleData
                    origin = CfirDeclarationOrigin.Synthetic.TypeAliasConstructor
                    containingDeclarationSymbol = newConstructorSymbol
                }
            }
        }
        typealiasConstructor.typeAliasConstructorInfo = TypeAliasConstructorInfo(
            originalConstructor = originalConstructor,
            typeAliasSymbol = typeAliasSymbol,
            substitutor = (delegatingScope as? CfirClassSubstitutionScope)?.substitutor,
        )
        return newConstructorSymbol
    }

    /**
     * 对齐 Kotlin FIR `TypeAliasConstructorsSubstitutingScope`：
     * typealias 构造器的真实返回类型仍是展开类型，但需要携带 alias abbreviation。
     * 这样无类型参数 alias（如 `type TypeC = Box<Int64>`）不会重新退回原类泛型推断。
     */
    private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.withTypeAliasAbbreviation(
        typeAliasSymbol: CfirTypeAliasSymbol,
    ): org.cangnova.cangjie.cfir.types.CfirTypeRef {
        if (!session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)) return this
        val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return this
        return buildResolvedTypeRefCopy(resolvedTypeRef) {
            coneType = resolvedTypeRef.coneType.withAbbreviation(
                AbbreviatedTypeAttribute(typeAliasSymbol.defaultType()),
            )
        }
    }

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? {
        return delegatingScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, it, newSession)
        }
    }
}
