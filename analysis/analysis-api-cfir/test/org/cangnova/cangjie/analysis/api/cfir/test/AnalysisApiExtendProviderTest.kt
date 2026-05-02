package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * `extend` 一等公开声明查询回归测试。
 *
 * 锁定新的 symbol provider 契约：
 * 1. 可按包直接查询顶层 extend；
 * 2. 可按目标 `ClassId` 查询其所有 extend；
 * 3. extend 自身的 declaredMemberScope 与 owner 语义保持稳定。
 */
class AnalysisApiExtendProviderTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/extendProvider",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun extendQueries(mainFile: CjFile) {
        val extendDeclaration = mainFile.declarations.filterIsInstance<CjExtend>().single()
        val classDeclaration = mainFile.declarations
            .filterIsInstance<CjTypeStatement>()
            .single { declaration -> declaration !is CjExtend && declaration.name == "Document" }

        analyzeForTest(mainFile) {
            val targetClass = classDeclaration.classSymbol as CaClassSymbol
            val packageExtends = getTopLevelExtendSymbols(mainFile.packageFqName)
            val classExtends = getExtendSymbols(targetClass.classId!!)
            val lightDeclarationProvider = CaLightDeclarationProvider.getInstance(mainFile.project)

            assertEquals(1, packageExtends.size)
            assertEquals(1, classExtends.size)

            val packageExtend = packageExtends.single()
            val classExtend = classExtends.single()

            assertEquals(extendDeclaration.getExtendId(), packageExtend.extendId)
            assertEquals(extendDeclaration.getExtendId(), classExtend.extendId)
            assertEquals(targetClass.classId, packageExtend.targetClassId)
            assertEquals(targetClass.classId, classExtend.targetClassId)

            assertEquals(
                "prettyPrint",
                packageExtend.declaredMemberScope.callables(Name.identifier("prettyPrint")).first().name?.asString(),
            )
            assertEquals(
                "prettyPrint",
                classExtend.declaredMemberScope.callables(Name.identifier("prettyPrint")).first().name?.asString(),
            )

            val restoredByPsi = extendDeclaration.symbol as CaExtendSymbol
            assertNotNull(restoredByPsi)
            assertEquals(restoredByPsi.extendId, packageExtend.extendId)
            assertEquals(restoredByPsi.extendId, classExtend.extendId)

            val extendLightDeclaration = lightDeclarationProvider.findLightDeclarations(
                packageFqName = mainFile.packageFqName,
                name = Name.identifier("Document"),
                useSiteModule = restoredByPsi.containingModule,
            ).filterIsInstance<CaLightExtendDeclaration>().singleOrNull()

            assertNotNull(extendLightDeclaration)
            assertEquals(restoredByPsi.extendId, extendLightDeclaration!!.extendId)
        }
    }
}
