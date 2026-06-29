package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(CompilerConfiguration.Internals::class)
/**
 * 覆盖前端宏构造服务在 strict/degraded 模式下的表面占位行为。
 */
class FrontendMacroConstructionServiceTest {
    /**
     * 验证 degraded 模式会为声明宏表面登记原始 surface 占位映射。
     */
    @Test
    fun degradedModeRegistersDeclarationSurfacePlaceholderWithoutFinalSurfaceNode() {
        val fixture = macroDeclarationSurfaceFixture()

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expandWithClassification(
            pre = fixture.pre,
            context = fixture.context,
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        assertTrue(result is MacroConstructionResult.Degraded)
        val degraded = result as MacroConstructionResult.Degraded
        assertSame(fixture.file, degraded.recordableFiles.files.single())
        assertEquals(fixture.surface.surfaceId, degraded.registry.placeholderOriginById[fixture.surface.surfaceId])
        assertSame(fixture.surface, degraded.registry.originSurfaceForPlaceholder(fixture.surface.surfaceId))
        assertTrue(
            degraded.registry.diagnostics.any { it.originSurfaceId == fixture.surface.surfaceId },
            "surface-only degraded diagnostics must keep original macro surface id",
        )
    }

    /**
     * 验证 strict 模式在没有稳定 splice 时拒绝声明宏表面。
     */
    @Test
    fun strictModeRejectsDeclarationSurfaceWithoutStableSplice() {
        val fixture = macroDeclarationSurfaceFixture()

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expandWithClassification(
            pre = fixture.pre,
            context = fixture.context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Failed)
    }

    /**
     * 构造一个带声明宏 surface 的最小 pre-macro fixture。
     */
    private fun macroDeclarationSurfaceFixture(): MacroDeclarationSurfaceFixture {
        val session = object : CfirSession(Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)

        val packageFqName = FqName("sample")
        val function = buildNamedFunction {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("annotated")))
            this.name = Name.identifier("annotated")
            isMut = false
            body = buildBlock {}
        }
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            this.name = "sample.cj"
            packageDirective = buildPackageDirective {
                this.packageFqName = packageFqName
                isMacroPackage = false
            }
            declarations += function
        }
        val surface = MacroSurfaceDecl(
            surfaceId = 2001L,
            qualifiedName = FqName.topLevel(Name.identifier("MissingAnnotationMacro")),
            kind = MacroSurface.Kind.PLAIN,
            hasParenthesis = true,
            attrTokens = emptyList(),
            inputTokens = emptyList(),
            sourceRange = null,
            scopeContext = MacroSurfaceScopeContext(
                packageFqName = packageFqName,
                enclosingClassFqName = null,
                enclosingFunctionName = null,
            ),
            modifiers = emptyList(),
            carriedAnnotations = listOf("@MissingAnnotationMacro()"),
            capturedRawSyntax = "@MissingAnnotationMacro()",
            containerContext = MacroSurfaceContainerContext(
                outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
                isInsidePrimaryConstructor = false,
                isInsideEnumBody = false,
                isInsideBlock = false,
            ),
            replaceHandle = CfirReplaceHandle(handleId = 2001L),
        )
        val pre = buildPreMacroRawFiles(
            session = session,
            rawCfirFiles = listOf(file),
            fileSurfaces = listOf(listOf(surface)),
        )

        return MacroDeclarationSurfaceFixture(
            file = file,
            surface = surface,
            pre = pre,
            context = bindMacroImports(pre, MacroSymbolIndex.EMPTY),
        )
    }

    /**
     * 声明宏 surface 测试夹具。
     */
    private data class MacroDeclarationSurfaceFixture(
        /**
         * 承载宏声明 surface 的 CFIR 文件。
         */
        val file: CfirFile,
        /**
         * 被测试的声明宏 surface。
         */
        val surface: MacroSurfaceDecl,
        /**
         * 预宏 raw 构建结果。
         */
        val pre: PreMacroRawBuildResult,
        /**
         * 与 [pre] 对应的宏解析上下文。
         */
        val context: MacroResolutionContext,
    )

    /**
     * 使用默认分类快照执行宏构造服务。
     */
    private fun FrontendMacroConstructionService.expandWithClassification(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        val classification = MacroDemandClassification.create(pre)
        classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
        return expand(pre, context, classification, mode)
    }

    /**
     * 测试用最小 CFIR module data。
     */
    private class TestModuleData(session: CfirSession) : CfirModuleData() {
        /**
         * 测试模块名称。
         */
        override val name: Name = Name.identifier("frontend-macro-construction-test")
        /**
         * 测试模块不声明普通依赖。
         */
        override val dependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块不声明 refinement 依赖。
         */
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块的全部 refinement 依赖为空。
         */
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块使用默认仓颉平台。
         */
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        /**
         * 测试模块使用默认 CFIR 平台。
         */
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        /**
         * 测试模块是否为 common 模块。
         */
        override val isCommon: Boolean = targetPlatform.isCommon()
        /**
         * 测试模块能力集合。
         */
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        /**
         * 测试模块稳定名称。
         */
        override val stableModuleName: String = "frontend-macro-construction-test"
        /**
         * 与该 module data 绑定的 session。
         */
        override val session: CfirSession = session

        init {
            bindSession(session)
        }
    }
}
