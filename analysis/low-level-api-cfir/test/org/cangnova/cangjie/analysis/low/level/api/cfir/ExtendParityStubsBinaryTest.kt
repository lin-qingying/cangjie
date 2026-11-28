@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories.LLBinaryOriginLibrarySymbolProviderFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS（物化后）与 BINARIES 的 extend 视图必须一致：
 * 同类型上不同成员集的多个 extend 不能被合并，成员集合必须相等。
 */
class ExtendParityStubsBinaryTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun extendParityStubsBinary(mainFile: CjFile) {
        val project = mainFile.project
        val mainModule = org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
            .getInstance(project)
            .getModule(mainFile, null)
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project)
            .getBuiltinsSession(mainModule.targetPlatform)

        val stubsProvider = builtinsSession.extendProviderOrNull
        assertNotNull(stubsProvider, "STUBS 下必须有 extend provider")
        val binarySymbolProvider =
            LLBinaryOriginLibrarySymbolProviderFactory.createBuiltinsExtendProvider(builtinsSession)
        val binaryProvider = CfirExtendProviderComposer.fromSymbolProviders(listOf(binarySymbolProvider))

        val cases: List<Pair<String, (org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider) -> List<CfirExtend>>> = listOf(
            "Int64" to { provider -> provider.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64) },
            "Float64" to { provider -> provider.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT64) },
            "Iterator" to { provider -> provider.getExtendsForClass(coreClass("Iterator")) },
        )
        cases.forEach { (name, query) ->
            val stubsExtends = query(stubsProvider!!)
            val binaryExtends = query(binaryProvider)
            assertEquals(binaryExtends.size, stubsExtends.size, "$name extend 数量必须一致")
            assertEquals(memberGroups(binaryExtends), memberGroups(stubsExtends), "$name 的 extend 成员分组必须一致")
            // 防止“物化失败静默回退”：STUBS 侧必须全部带 PSI。
            assertTrue(stubsExtends.all { extend -> extend.source != null }, "$name 的 extend 必须全部物化（不能静默回退）")
            assertTrue(
                stubsExtends.flatMap { extend -> extend.declarations.filterIsInstance<CfirCallableDeclaration>() }
                    .all { declaration -> declaration.source != null },
                "$name 的 extend 成员必须全部带 PSI",
            )
        }
        assertEquals(
            3,
            stubsProvider!!.getExtendsForClass(coreClass("Iterator")).mapNotNull { extend -> extend.source }.distinct().size,
            "Iterator 的 3 个 extend 必须各自对应不同 PSI",
        )

        val float64Stubs = stubsProvider!!.getExtendsForBuiltinType(
            org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT64,
        )
        val fromBits = float64Stubs.asSequence()
            .flatMap { extend -> extend.declarations.filterIsInstance<CfirCallableDeclaration>().asSequence() }
            .firstOrNull { declaration ->
                declaration.symbol.callableId.callableName.asString() == "fromBits"
            }
        assertNotNull(
            fromBits,
            "Float64 的 fromBits 不能被合并丢失；members=" + float64Stubs.flatMap { extend ->
                extend.declarations.filterIsInstance<CfirCallableDeclaration>()
            }.map { declaration -> declaration.symbol.callableId.callableName.asString() },
        )
        assertTrue(fromBits!!.status.isStatic, "Float64.fromBits 必须是 static")
        assertNotNull(fromBits.source, "物化后的 fromBits 必须带 PSI")
    }

    private fun coreClass(name: String): ClassId =
        ClassId(FqName("std.core"), org.cangnova.cangjie.name.Name.identifier(name))

    /** 每个 extend 的成员集合（按 kind|name 排序），整体再排序后用于比较分组。 */
    private fun memberGroups(extends: List<CfirExtend>): List<String> = extends.map { extend ->
        extend.declarations
            .filterIsInstance<CfirCallableDeclaration>()
            .map { declaration ->
                val kind = declaration::class.simpleName.orEmpty().removeSuffix("Impl")
                "$kind|${declaration.symbol.callableId.callableName.asString()}"
            }
            .sorted()
            .joinToString("&")
    }.sorted()
}
