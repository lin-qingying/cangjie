package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(CfirImplementationDetail::class)
/**
 * 验证 CFIR 声明的 toString 输出委托给可读性 renderer，而不是对象默认表示。
 */
class CfirDeclarationToStringTest {
    /**
     * 验证类声明的 toString 与可读性 renderer 输出一致。
     */
    @Test
    fun `class toString delegates to readability renderer`() {
        val classId = org.cangnova.cangjie.name.ClassId(FqName("sample"), Name.identifier("Box"))
        val declaration = buildClass {
            source = TestBinarySourceElement("class Box")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = TestScopeProvider
            status = CfirDeclarationStatusImpl()
            symbol = CfirClassSymbol(classId)
            name = Name.identifier("Box")
        }

        declaration.symbol.bind(declaration)

        assertMatchesReadabilityRenderer(declaration)
    }

    /**
     * 验证具名函数声明的 toString 与可读性 renderer 输出一致。
     */
    @Test
    fun `named function toString delegates to readability renderer`() {
        val callableId = CallableId(FqName("sample"), Name.identifier("compute"))
        val functionSymbol = CfirNamedFunctionSymbol(callableId)
        val declaration = buildNamedFunction {
            source = TestBinarySourceElement("func compute")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = functionSymbol
            name = Name.identifier("compute")
            isMut = false
            valueParameters += buildValueParameter {
                source = TestBinarySourceElement("param value")
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                symbol = CfirValueParameterSymbol(CallableId(Name.identifier("value")))
                containingDeclarationSymbol = functionSymbol
                isNamed = false
                status = CfirDeclarationStatusImpl()
                returnTypeRef = buildImplicitTypeRef()
                name = Name.identifier("value")
            }.also { it.symbol.bind(it) }
        }

        declaration.symbol.bind(declaration)

        assertMatchesReadabilityRenderer(declaration)
    }

    /**
     * 验证文件声明的 toString 与可读性 renderer 输出一致。
     */
    @Test
    fun `file toString delegates to readability renderer`() {
        val file = buildFile {
            source = TestBinarySourceElement("file sample.cj")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                packageFqName = FqName("sample")
                isMacroPackage = false
            }
        }

        file.symbol.bind(file)

        assertMatchesReadabilityRenderer(file)
    }

    /**
     * 断言声明 toString 与 CfirRenderer.withReadability 渲染结果一致。
     */
    private fun assertMatchesReadabilityRenderer(declaration: CfirDeclaration) {
        val expected = CfirRenderer.withReadability().renderElementAsString(declaration)
        assertEquals(expected, declaration.toString())
    }

    /**
     * toString 测试使用的源码 session。
     */
    private object TestSession : CfirSession(Kind.Source) {
        /**
         * 返回稳定的测试 session 名称。
         */
        override fun toString(): String = "CfirDeclarationToStringTestSession"
    }

    /**
     * 带稳定 debug 文本的测试二进制源码元素。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )

    /**
     * 返回空类型作用域的测试 scope provider。
     */
    private object TestScopeProvider : CfirScopeProvider() {
        /**
         * 类使用点 member scope 在本测试中为空。
         */
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty

        /**
         * renderer 测试不解析 typealias 构造调用，返回空 scope 保持测试边界纯粹。
         */
        override fun getTypealiasConstructorScope(
            typeAlias: CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty

        /**
         * 类声明点 member scope 在本测试中为空。
         */
        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty
    }

    /**
     * toString 测试使用的模块数据。
     */
    private object TestModuleData : CfirModuleData() {
        /**
         * 测试模块名。
         */
        override val name: Name = Name.identifier("cfir-tree-test")
        /**
         * 测试模块无普通依赖。
         */
        override val dependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块无 refinement 依赖。
         */
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块无传递 refinement 依赖。
         */
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 使用默认仓颉平台。
         */
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        /**
         * 使用默认 CFIR 平台。
         */
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        /**
         * 标记模块是否为 common 平台。
         */
        override val isCommon: Boolean = targetPlatform.isCommon()
        /**
         * 测试模块不声明额外能力。
         */
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        /**
         * 稳定模块名。
         */
        override val stableModuleName: String = "cfir-tree-test"
        /**
         * 绑定到测试源码 session。
         */
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }
}
