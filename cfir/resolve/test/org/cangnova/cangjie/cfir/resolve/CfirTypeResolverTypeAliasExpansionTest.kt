@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProviderWithoutCallables
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * 独立锁定 `cfir:resolve` 对 typealias 的底层语义：
 * 当调用方显式要求 `expandTypeAliases = false` 时，resolver 必须保留 `ConeTypeAliasType`，
 * 不能在底层把声明侧 typealias 直接抹平成展开后的基础类型。
 */
class CfirTypeResolverTypeAliasExpansionTest {
    /**
     * 验证调用方关闭 typealias 展开时保留 [ConeTypeAliasType]。
     */
    @Test
    fun `resolveType preserves typealias when expansion is disabled`() {
        val aliasClassId = ClassId(FqName("sample.lib"), Name.identifier("RemoteAlias"))
        val resolver = createResolverForTypeAlias(aliasClassId)
        val typeRef = buildUserTypeRef {
            source = TestBinarySourceElement("sample.lib.RemoteAlias")
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = Name.identifier("sample")
            }
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = Name.identifier("lib")
            }
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = aliasClassId.shortClassName
            }
        }

        val preservedType = resolver.resolveType(
            typeRef = typeRef,
            configuration = TypeResolutionConfiguration.EMPTY,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = false,
        ).type

        val aliasType = assertInstanceOf(ConeTypeAliasType::class.java, preservedType)
        assertEquals(aliasClassId, aliasType.classId)
        assertEquals(ConePrimitiveType.INT64, aliasType.expandedType)
        assertFalse(aliasType is ConeErrorType, "保留 typealias 语义时不能退化成错误类型。")

        val expandedType = resolver.resolveType(
            typeRef = typeRef,
            configuration = TypeResolutionConfiguration.EMPTY,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = true,
        ).type

        assertEquals(ConePrimitiveType.INT64, expandedType)
        assertFalse(expandedType is ConeTypeAliasType, "允许展开时必须返回展开后的真实类型。")
        assertFalse(expandedType is ConeErrorType, "展开后的真实类型不能退化成错误类型。")
    }

    /**
     * 验证全局禁用 typealias 展开会覆盖局部展开请求。
     */
    @Test
    fun `global no alias expansion flag overrides local expansion request`() {
        val aliasClassId = ClassId(FqName("sample.lib"), Name.identifier("RemoteAlias"))
        val resolver = createResolverForTypeAlias(
            aliasClassId,
            languageVersionSettings = LanguageVersionSettings(
                analysisFlags = mapOf(AnalysisFlags.expandTypeAliasesInTypeResolution to false),
            ),
        )
        val typeRef = buildUserTypeRef {
            source = TestBinarySourceElement("sample.lib.RemoteAlias")
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = Name.identifier("sample")
            }
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = Name.identifier("lib")
            }
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = aliasClassId.shortClassName
            }
        }

        val resolvedType = resolver.resolveType(
            typeRef = typeRef,
            configuration = TypeResolutionConfiguration.EMPTY,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = true,
        ).type

        val aliasType = assertInstanceOf(ConeTypeAliasType::class.java, resolvedType)
        assertEquals(aliasClassId, aliasType.classId)
        assertEquals(ConePrimitiveType.INT64, aliasType.expandedType)
    }

    /**
     * 构造只包含一个 typealias 声明的测试 type resolver。
     */
    private fun createResolverForTypeAlias(
        aliasClassId: ClassId,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettings.DEFAULT,
    ): CfirTypeResolverImpl {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule("typealias-expansion")
        session.register(
            CfirLanguageSettingsComponent::class,
            CfirLanguageSettingsComponent(languageVersionSettings),
        )
        session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        session.register(
            CfirDefaultImportsProviderHolder::class,
            CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider),
        )

        val aliasDeclaration = buildTypeAlias {
            source = TestBinarySourceElement("type RemoteAlias = Int64")
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = TestScopeProvider
            symbol = CfirTypeAliasSymbol(aliasClassId)
            status = CfirDeclarationStatusImpl()
            name = aliasClassId.shortClassName
            expandedTypeRef = buildBasicTypeRef {
                source = TestBinarySourceElement("Int64")
                name = Name.identifier("Int64")
            }
        }

        val symbolProvider = TestTypeAliasSymbolProvider(session, aliasDeclaration)
        session.register(CfirSymbolProvider::class, symbolProvider)
        session.register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))
        return CfirTypeResolverImpl(session)
    }

    /**
     * 为测试 typealias 提供 class-like symbol 查询能力。
     */
    private class TestTypeAliasSymbolProvider(
        session: CfirSession,
        /**
         * 当前 provider 暴露的 typealias 声明。
         */
        private val typeAlias: CfirTypeAlias,
    ) : CfirSymbolProvider(session) {
        /**
         * 测试 typealias 的 ClassId。
         */
        private val aliasClassId: ClassId = typeAlias.symbol.classId

        /**
         * 仅暴露测试 typealias 所在包和短名的 symbol names provider。
         */
        override val symbolNamesProvider = object : CfirSymbolNamesProviderWithoutCallables() {
            override fun getPackageNames(): Set<String> = setOf(aliasClassId.packageFqName.asString())

            override val hasSpecificClassifierPackageNamesComputation: Boolean
                get() = true

            override fun getPackageNamesWithTopLevelClassifiers(): Set<String> =
                setOf(aliasClassId.packageFqName.asString())

            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                if (packageFqName == aliasClassId.packageFqName) {
                    setOf(aliasClassId.shortClassName)
                } else {
                    emptySet()
                }
        }

        /**
         * 按 ClassId 返回测试 typealias symbol。
         */
        override fun getClassLikeSymbolByClassId(classId: ClassId) =
            typeAlias.symbol.takeIf { classId == aliasClassId }

        /**
         * 测试 provider 不暴露 callable symbol。
         */
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        /**
         * 测试 provider 不暴露函数 symbol。
         */
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        /**
         * 测试 provider 不暴露属性 symbol。
         */
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        /**
         * 只承认 typealias 所在包存在。
         */
        override fun hasPackage(fqName: FqName): Boolean = fqName == aliasClassId.packageFqName
    }

    /**
     * typealias 测试不需要成员 scope，统一返回空 scope。
     */
    private object TestScopeProvider : CfirScopeProvider() {
        /**
         * 返回空的 use-site 成员 scope。
         */
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty

        /**
         * 返回空的 declaration-site 成员 scope。
         */
        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty
    }

    /**
     * 带稳定 debug identity 的二进制 source element。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )
}
