@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedAnnotationDeserializer
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirDeserializationContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirExtendBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirTypeDeserializer
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * 从 decompiled stub PSI 构建的 extend 必须带 PSI 来源与带 PSI 的成员，
 * 且隐式类型参数 `T` 可解析（复用 `.cjo` 反序列化 extend 的类型参数）。
 */
class StubBasedCfirExtendPsiTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun stubBasedExtendPsi(mainFile: CjFile) {
        val project = mainFile.project
        val mainModule = org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
            .getInstance(project)
            .getModule(mainFile, null)
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project)
            .getBuiltinsSession(mainModule.targetPlatform)

        val rawBinaryProvider = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer.fromSymbolProviders(
            listOf(
                org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories
                    .LLBinaryOriginLibrarySymbolProviderFactory.createBuiltinsExtendProvider(builtinsSession),
            ),
        )
        val binaryExtend = rawBinaryProvider.getExtendsForClass(StdlibClassIds.Array)
            .first { extend -> extend.declarations.any { it is CfirProperty && it.name.asString() == "size" } }
        org.junit.jupiter.api.Assertions.assertNull(binaryExtend.source, "原始二进制 extend 不应带 PSI")

        val stdCoreFile = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(project)
            .mapNotNull { virtualFile -> PsiManager.getInstance(project).findFile(virtualFile) }
            .filterIsInstance<CjFile>()
            .first { file -> file.name.contains("std.core") }

        val extendPsi = PsiTreeUtil.findChildrenOfType(stdCoreFile, CjExtend::class.java)
            .first { extend -> extend.text.contains("Collection") && extend.text.contains("Array") }

        val annotationDeserializer = StubBasedAnnotationDeserializer(builtinsSession)
        val context = StubBasedCfirDeserializationContext(
            moduleData = builtinsSession.moduleData,
            packageFqName = FqName("std.core"),
            relativeClassName = null,
            typeDeserializer = StubBasedCfirTypeDeserializer(
                builtinsSession.moduleData,
                annotationDeserializer,
                parent = null,
                containingSymbol = null,
                owner = null,
                initialOrigin = CfirDeclarationOrigin.Library,
            ),
            annotationDeserializer = annotationDeserializer,
            containerSource = null,
            outerClassSymbol = null,
            outerTypeParameters = emptyList(),
            initialOrigin = CfirDeclarationOrigin.Library,
        )

        val extend: CfirExtend = StubBasedCfirExtendBuilder(context).build(extendPsi, binaryExtend!!)
        assertNotNull(extend.source, "stub-based extend 必须带 PSI 来源")
        assertEquals(1, extend.superTypeRefs.size)
        assertEquals(listOf("T"), extend.typeParameters.map { it.name.asString() })

        val size = extend.declarations
            .filterIsInstance<CfirProperty>()
            .single { declaration -> declaration.name.asString() == "size" }
        assertNotNull(size.source, "prop size 必须带 PSI 来源")
        // 已知限制（见 docs/known-issues-stubs-extend.md）：物化 extend 复用二进制 typeParameters，
        // 其 owner 仍指向二进制 extend。这里 pin 住现状，避免无提示地改变归属语义。
        org.junit.jupiter.api.Assertions.assertSame(
            binaryExtend.symbol,
            extend.typeParameters.first().containingDeclarationSymbol,
            "typeParameters 复用二进制声明（已知限制）",
        )
    }
}
