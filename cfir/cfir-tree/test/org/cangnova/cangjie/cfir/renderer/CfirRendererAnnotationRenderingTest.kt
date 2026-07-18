package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildProperty
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildFunctionTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(CfirImplementationDetail::class)
/**
 * 验证 CFIR renderer 对声明注解、类型注解和注解实参的渲染格式。
 */
class CfirRendererAnnotationRenderingTest {
    /**
     * 验证文件、类、属性、函数和值参数上的注解都会被 golden renderer 输出。
     */
    @Test
    fun `renders annotations on file declarations and value parameters`() {
        val fileSymbol = CfirFileSymbol()
        val classId = ClassId(FqName("sample"), Name.identifier("Annotated"))
        val propertyId = CallableId(FqName("sample"), Name.identifier("value"))
        val functionId = CallableId(FqName("sample"), Name.identifier("compute"))
        val parameterId = CallableId(Name.identifier("input"))
        val functionSymbol = CfirNamedFunctionSymbol(functionId)

        val file = buildFile {
            source = TestBinarySourceElement("file annotated.cj")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = fileSymbol
            name = "annotated.cj"
            packageDirective = buildPackageDirective {
                packageFqName = FqName("sample")
                isMacroPackage = false
            }
            annotations += annotation("FileAnn")
            declarations += buildClass {
                source = TestBinarySourceElement("class Annotated")
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                scopeProvider = TestScopeProvider
                status = CfirDeclarationStatusImpl()
                symbol = CfirClassSymbol(classId)
                name = Name.identifier("Annotated")
                annotations += annotation("ClassAnn")
            }.also { it.symbol.bind(it) }
            declarations += buildProperty {
                source = TestBinarySourceElement("prop value")
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                symbol = CfirPropertySymbol(propertyId)
                status = CfirDeclarationStatusImpl()
                returnTypeRef = basicType("Int")
                name = Name.identifier("value")
                annotations += annotation("PropAnn")
            }.also { it.symbol.bind(it) }
            declarations += buildNamedFunction {
                source = TestBinarySourceElement("func compute")
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                status = CfirDeclarationStatusImpl()
                returnTypeRef = basicType("Int")
                symbol = functionSymbol
                name = Name.identifier("compute")
                isMut = false
                annotations += annotation("FuncAnn")
                valueParameters += buildValueParameter {
                    source = TestBinarySourceElement("param input")
                    moduleData = TestModuleData
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    dispatchReceiverType = null
                    symbol = CfirValueParameterSymbol(parameterId)
                    containingDeclarationSymbol = functionSymbol
                    isNamed = false
                    status = CfirDeclarationStatusImpl()
                    returnTypeRef = basicType("Int")
                    name = Name.identifier("input")
                    annotations += annotation("ParamAnn")
                }.also { it.symbol.bind(it) }
            }.also { it.symbol.bind(it) }
        }

        file.symbol.bind(file)

        val rendered = CfirRenderer.withGoldenCompat().renderElementAsString(file)
        assertTrue(rendered.contains("@R|FileAnn|()"))
        assertTrue(rendered.contains("@R|ClassAnn|()"))
        assertTrue(rendered.contains("@R|PropAnn|()"))
        assertTrue(rendered.contains("@R|FuncAnn|()"))
        assertTrue(rendered.contains("@R|ParamAnn|()"))
        assertTrue(rendered.contains("input: R|Int|"))
    }

    /**
     * 验证注解调用的普通实参和嵌套注解实参会以内联形式渲染。
     */
    @Test
    fun `renders annotation call arguments and nested annotation arguments inline`() {
        val rendered = CfirRenderer.withGoldenCompat().renderElementAsString(
            annotation(
                name = "OuterAnn",
                arguments = listOf(
                    literal(1, CfirLiteralKind.INT),
                    annotation("InnerAnn", listOf(literal("nested", CfirLiteralKind.STRING))),
                ),
            )
        )

        assertEquals("""@R|OuterAnn|(1, @R|InnerAnn|("nested"))""", rendered)
    }

    /**
     * 验证函数类型和用户类型引用上的注解会出现在类型渲染文本中。
     */
    @Test
    fun `renders annotations on function type and user type refs`() {
        val functionType = buildFunctionTypeRef {
            annotations += annotation("FnAnn")
            parameterTypeRefs += basicType("Int")
            returnTypeRef = basicType("String")
        }
        val userType = buildUserTypeRef {
            source = TestBinarySourceElement("user-type")
            annotations += annotation("UserAnn")
            qualifier += buildQualifierPart {
                name = Name.identifier("demo")
            }
            qualifier += buildQualifierPart {
                name = Name.identifier("Box")
                typeArguments += basicType("Int")
            }
        }

        assertEquals("@R|FnAnn|() R|(R|Int|) -> R|String||", CfirRenderer.withGoldenCompat().renderElementAsString(functionType))
        assertEquals("@R|UserAnn|() R|demo.Box<R|Int|>|", CfirRenderer.withGoldenCompat().renderElementAsString(userType))
    }

    /**
     * 验证同一个 renderer 实例重复渲染不同元素时不会串联上一次状态。
     */
    @Test
    fun `reused renderer renders each element independently`() {
        val renderer = CfirRenderer.withGoldenCompat()
        val first = renderer.renderElementAsString(annotation("FirstAnn"))
        val second = renderer.renderElementAsString(annotation("SecondAnn"))

        assertEquals("@R|FirstAnn|()", first)
        assertEquals("@R|SecondAnn|()", second)
    }

    /**
     * 构造测试注解调用表达式。
     */
    private fun annotation(name: String, arguments: List<CfirElement> = emptyList()): CfirAnnotationCall =
        buildAnnotationCall {
            source = TestBinarySourceElement("@$name")
            typeRef = basicType(name)
            this.arguments += arguments
            argumentList = buildArgumentList {
                this.arguments += arguments.filterIsInstance<CfirExpression>()
            }
            calleeReference = buildNamedReference {
                this.name = Name.identifier(name)
            }
            containingDeclarationSymbol = CfirFileSymbol()
        }

    /**
     * 构造测试基础类型引用。
     */
    private fun basicType(name: String) = buildBasicTypeRef {
        source = TestBinarySourceElement("type $name")
        this.name = Name.identifier(name)
    }

    /**
     * 构造测试字面量表达式。
     */
    private fun literal(value: Any, kind: CfirLiteralKind) = buildLiteralExpression {
        this.value = value
        this.kind = kind
    }

    /**
     * 注解 renderer 测试使用的源码 session。
     */
    private object TestSession : CfirSession(Kind.Source) {
        /**
         * 返回稳定的测试 session 名称。
         */
        override fun toString(): String = "CfirRendererAnnotationRenderingTestSession"
    }

    /**
     * 返回空类型作用域的测试 scope provider。
     */
    private object TestScopeProvider : CfirScopeProvider() {
        /**
         * 类使用点 member scope 在本测试中为空。
         */
        override fun getUseSiteMemberScope(
            klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
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
            klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty
    }

    /**
     * 注解 renderer 测试使用的模块数据。
     */
    private object TestModuleData : CfirModuleData() {
        /**
         * 测试模块名。
         */
        override val name: Name = Name.identifier("cfir-renderer-annotation-test")
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
        override val stableModuleName: String = "cfir-renderer-annotation-test"
        /**
         * 绑定到测试源码 session。
         */
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    /**
     * 带稳定 debug 文本的测试二进制源码元素。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )
}
