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

/**
 * typealias 构造器合成信息属性 key。
 */
private object TypeAliasConstructorInfoKey : CfirDeclarationDataKey()

/**
 * typealias 构造器合成信息。
 *
 * @property originalConstructor 展开类型上的原始构造器。
 * @property typeAliasSymbol 触发构造器合成的 typealias symbol。
 * @property substitutor 展开类型 scope 使用的类型替换器。
 */
data class TypeAliasConstructorInfo<T : CfirFunction>(
    /**
     * 展开类型上的原始构造器声明。
     */
    val originalConstructor: T,
    /**
     * 触发构造器合成的 typealias symbol。
     */
    val typeAliasSymbol: CfirTypeAliasSymbol,
    /**
     * 展开类型 scope 使用的类型替换器。
     */
    val substitutor: ConeSubstitutor?,
)

/**
 * 构造器声明上携带的 typealias 构造器合成信息。
 */
var <T : CfirFunction> T.typeAliasConstructorInfo: TypeAliasConstructorInfo<T>? by CfirDeclarationDataRegistry.data(TypeAliasConstructorInfoKey)

/**
 * 构造器 symbol 对应的 typealias 构造器合成信息。
 */
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
    /**
     * 当前 typealias symbol。
     */
    private val typeAliasSymbol: CfirTypeAliasSymbol,
    /**
     * 展开类型的构造器来源 scope。
     */
    private val delegatingScope: CfirScope,
    /**
     * 当前 use-site session。
     */
    private val session: CfirSession,
) : CfirScope() {
    /**
     * typealias 构造器替换 scope 工厂。
     */
    companion object {
        /**
         * 为 [typeAliasSymbol] 创建构造器替换 scope。
         */
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

    /**
     * 原始构造器到合成 typealias 构造器的缓存。
     */
    private val constructorCache = mutableMapOf<CfirConstructorSymbol, CfirConstructorSymbol>()

    /**
     * 处理展开类型构造器，并映射为 typealias 构造器。
     */
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors { originalConstructorSymbol ->
            processor(constructorCache.getOrPut(originalConstructorSymbol) {
                createTypealiasConstructor(originalConstructorSymbol)
            })
        }
    }

    /**
     * 基于展开类型原始构造器创建 typealias 构造器副本。
     */
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

    /**
     * 替换委托 scope 的 session 后重建 typealias 构造器 scope。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? {
        return delegatingScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            TypeAliasConstructorsSubstitutingScope(typeAliasSymbol, it, newSession)
        }
    }
}
