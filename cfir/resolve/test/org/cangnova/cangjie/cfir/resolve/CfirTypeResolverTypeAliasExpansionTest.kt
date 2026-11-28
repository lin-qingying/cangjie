@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
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
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
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
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.withAbbreviation
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
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
        val aliasClassId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val resolver = createResolverForTypeAlias(aliasClassId)
        val typeRef = buildUserTypeRef {
            source = testSourceElement()
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
        val aliasClassId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val resolver = createResolverForTypeAlias(
            aliasClassId,
            languageVersionSettings = LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.LATEST_STABLE,
                analysisFlags = mapOf(AnalysisFlags.expandTypeAliasesInTypeResolution to false),
            ),
        )
        val typeRef = buildUserTypeRef {
            source = testSourceElement()
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

    /** SUPER_TYPES 的临时展开产品必须覆盖类型节点缓存的旧展开结果。 */
    @Test
    fun `direct expansion uses supplied alias product instead of cached expansion`() {
        val aliasClassId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val session = createSessionForTypeAlias(aliasClassId)
        val aliasType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT64)
        val aliasSymbol = session.symbolProvider.getClassLikeSymbolByClassId(aliasClassId)
        var expansionReads = 0

        val expansion = aliasType.directExpansionType(session) { alias ->
            assertEquals(aliasSymbol, alias.symbol)
            expansionReads++
            ConePrimitiveType.FLOAT64
        }

        assertEquals(ConePrimitiveType.FLOAT64, expansion)
        assertEquals(1, expansionReads)
    }

    /** Supplier 暂未发布展开产品时，禁止从旧缓存或声明 typeRef 绕过该边界。 */
    @Test
    fun `direct expansion preserves missing supplied alias product`() {
        val aliasClassId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val session = createSessionForTypeAlias(aliasClassId)
        val aliasType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT64)
        var expansionReads = 0

        assertNull(aliasType.directExpansionType(session) {
            expansionReads++
            null
        })
        assertEquals(1, expansionReads)
    }

    /** alias 本体、属性和递归子类型都不能用旧节点展开值越过所选来源的产品边界。 */
    @Test
    fun `abbreviation expansion preserves missing selected products throughout the type tree`() {
        val aliasId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val session = createSessionForTypeAlias(aliasId)
        val alias = ConeTypeAliasType(aliasId, expandedType = ConePrimitiveType.FLOAT64)
        val carrier = ConePrimitiveType.INT64.withAbbreviation(
            org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute(alias),
        )
        val nested = org.cangnova.cangjie.cfir.types.ConeTupleType(listOf(alias, carrier))
        val missing = org.cangnova.cangjie.cfir.resolve.providers.CfirEmptySymbolProvider(session)
        for (symbols in listOf(missing, session.symbolProvider)) {
            fun expand(type: org.cangnova.cangjie.cfir.types.ConeCangJieType) =
                type.fullyExpandedTypeUsingAbbreviation(session, symbols) { null }
            fun assertUnexpanded(type: org.cangnova.cangjie.cfir.types.ConeCangJieType) {
                val restored = assertInstanceOf(ConeTypeAliasType::class.java, type)
                // 属性重写可生成新节点；未展开 alias 的身份与保留字段必须完整一致。
                assertEquals(alias.classId, restored.classId)
                assertEquals(alias.typeArguments, restored.typeArguments)
                assertEquals(alias.expandedType, restored.expandedType)
            }
            assertSame(alias, expand(alias))
            assertUnexpanded(expand(carrier))
            val restoredElements = (expand(nested) as org.cangnova.cangjie.cfir.types.ConeTupleType).elementTypes
            assertEquals(2, restoredElements.size)
            restoredElements.forEach(::assertUnexpanded)
        }
        assertEquals(ConePrimitiveType.INT64, alias.fullyExpandedTypeUsingAbbreviation(session))
    }

    /** 从语法类型引用进入解析器时，阶段 Supplier 仍决定 alias 产品。 */
    @Test
    fun `user type resolution forwards supplied alias product`() {
        val aliasClassId = ClassId(FqName.ROOT, Name.identifier("RemoteAlias"))
        val session = createSessionForTypeAlias(aliasClassId)
        val resolver = CfirTypeResolverImpl(session)
        val typeRef = buildUserTypeRef {
            source = testSourceElement()
            qualifier += buildQualifierPart {
                source = this@buildUserTypeRef.source
                name = aliasClassId.shortClassName
            }
        }
        val supplier = object : SupertypeSupplier() {
            override fun forClass(firClass: CfirClassLikeDeclaration, useSiteSession: CfirSession): List<ConeCangJieType> =
                SupertypeSupplier.Default.forClass(firClass, useSiteSession)

            override fun expansionForTypeAlias(typeAlias: CfirTypeAlias, useSiteSession: CfirSession): ConeCangJieType =
                ConePrimitiveType.FLOAT64
        }

        val resolvedType = resolver.resolveType(
            typeRef, TypeResolutionConfiguration.EMPTY,
            areBareTypesAllowed = false, isOperandOfIsOperator = false, resolveDeprecations = true,
            supertypeSupplier = supplier, expandTypeAliases = true,
        ).type

        val primitive = assertInstanceOf(ConePrimitiveType::class.java, resolvedType)
        assertEquals(ConePrimitiveType.FLOAT64.kind, primitive.kind)
    }

    /** 构造只包含一个 typealias 声明的测试 type resolver。 */
    private fun createResolverForTypeAlias(
        aliasClassId: ClassId,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    ): CfirTypeResolverImpl = CfirTypeResolverImpl(createSessionForTypeAlias(aliasClassId, languageVersionSettings))

    private fun createSessionForTypeAlias(
        aliasClassId: ClassId,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
    ): CfirSession {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule("typealias-expansion")
        session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
        session.register(
            CfirLanguageSettingsComponent::class,
            CfirLanguageSettingsComponent(languageVersionSettings),
        )
        session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        session.register(
            org.cangnova.cangjie.cfir.declarations.CfirDeclarationAvailabilityProvider::class,
            org.cangnova.cangjie.cfir.declarations.CfirDeclarationAvailabilityProvider(session),
        )
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
            expandedTypeRef = org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl(
                source = null,
                annotations = org.cangnova.cangjie.cfir.MutableOrEmptyList.empty(),
                customRenderer = false,
                typeUseOrigin = org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC,
                coneType = ConePrimitiveType.INT64,
                delegatedTypeRef = null,
            )
        }

        val symbolProvider = TestTypeAliasSymbolProvider(session, aliasDeclaration)
        session.register(CfirSymbolProvider::class, symbolProvider)
        session.register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))
        session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
        return session
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
            memberRequiredPhase: CfirResolvePhase?,
        ): CfirTypeScope = CfirTypeScope.Empty

        /**
         * 返回空的 declaration-site 成员 scope。
         */
        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty

        /**
         * typealias 测试不需要构造器 scope，统一返回空 scope。
         */
        override fun getTypealiasConstructorScope(
            typeAlias: CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): org.cangnova.cangjie.cfir.scopes.CfirScope = CfirTypeScope.Empty
    }

    /**
     * 带稳定 debug identity 的二进制 source element。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )

    /** 用 LightTree 形态构造真实的测试类型引用来源。 */
    private fun testSourceElement(): CjSourceElement = CjLightSourceElement(
        lighterASTNode = TEST_LIGHTER_AST_NODE,
        startOffset = 0,
        endOffset = 1,
        treeStructure = TEST_TREE_STRUCTURE,
        kind = CjRealSourceElementKind,
    )

    private val TEST_LIGHTER_AST_NODE = object : LighterASTNode {
        override fun getTokenType(): IElementType = TokenType.DUMMY_HOLDER
        override fun getStartOffset(): Int = 0
        override fun getEndOffset(): Int = 1
    }

    private val TEST_TREE_STRUCTURE = object : FlyweightCapableTreeStructure<LighterASTNode> {
        override fun getRoot(): LighterASTNode = TEST_LIGHTER_AST_NODE
        override fun getParent(node: LighterASTNode): LighterASTNode? = null
        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int = 0
        override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}
        override fun getStartOffset(node: LighterASTNode): Int = node.startOffset
        override fun getEndOffset(node: LighterASTNode): Int = node.endOffset
        override fun toString(node: LighterASTNode): CharSequence = "RemoteAlias"
    }
}
