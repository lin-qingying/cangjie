@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedAnnotationDeserializer
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirDeserializationContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirExtendBuilder
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubBasedCfirTypeDeserializer
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 包级 extend PSI 发现必须可用，且能与二进制 extend 配对物化成带 PSI 的 CFIR extend。
 *
 * 这是 provider 接线的可行性验证：`CangJieDeclarationProvider.getPackageFiles` 提供包内
 * decompiled 文件，从中可找到 `CjExtend`；配对 `.cjo` 反序列化 extend 后交给 builder。
 */
class ExtendPsiDiscoveryTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun extendPsiDiscovery(mainFile: CjFile) {
        val project = mainFile.project
        val mainModule = org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
            .getInstance(project)
            .getModule(mainFile, null)
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project)
            .getBuiltinsSession(mainModule.targetPlatform)
        val builtinsModule = builtinsSession.moduleData.caModule

        val declarationProvider = CangJieDeclarationProviderFactory.getInstance(project)
            .createDeclarationProvider(builtinsModule.contentScope, builtinsModule)
        val stdCoreFiles = declarationProvider.getPackageFiles(FqName("std.core"))
        assertTrue(stdCoreFiles.isNotEmpty(), "std.core 包必须能枚举出 decompiled 文件")

        val extendPsi = stdCoreFiles.asSequence()
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, CjExtend::class.java).asSequence() }
            .firstOrNull { extend -> extend.text.contains("Collection") && extend.text.contains("Array") }
        assertNotNull(extendPsi, "std.core 文件必须包含 extend Array<T> <: Collection PSI")

        val rawBinaryProvider = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer.fromSymbolProviders(
            listOf(
                org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories
                    .LLBinaryOriginLibrarySymbolProviderFactory.createBuiltinsExtendProvider(builtinsSession),
            ),
        )
        val binaryExtend = rawBinaryProvider.getExtendsForClass(StdlibClassIds.Array)
            .first { extend -> extend.declarations.any { it is CfirProperty && it.name.asString() == "size" } }
        org.junit.jupiter.api.Assertions.assertNull(binaryExtend.source, "原始二进制 extend 不应带 PSI")

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
        val materialized = StubBasedCfirExtendBuilder(context).build(extendPsi!!, binaryExtend!!)

        assertNotNull(materialized.source, "物化后的 extend 必须带 PSI")
        val size = materialized.declarations
            .filterIsInstance<CfirProperty>()
            .single { declaration -> declaration.name.asString() == "size" }
        assertNotNull(size.source, "物化后的 size 必须带 PSI")
    }
}
