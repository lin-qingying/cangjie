package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedCallableId
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedClassId
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedFileSymbolName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedFileSymbolPackage
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedPackageSymbolFqName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * 顶层符号提供器抽象测试。
 *
 * 这里覆盖公开 `CaSymbolProvider` 的基础语义：
 * 1. `fileSymbol()` 必须稳定反映当前文件与包名；
 * 2. `getPackageSymbol()` 必须能恢复 use-site 可见的包符号；
 * 3. 顶层 class-like / callable 查询必须返回稳定的公开标识。
 */
abstract class AbstractTopLevelSymbolProviderTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)

        analyzeForTest(mainFile) {
            val fileSymbol = mainFile.fileSymbol()
            val packageSymbol = getPackageSymbol(mainFile.packageFqName)
            val classLikeSymbol = getTopLevelClassLikeSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetClassName),
            ).singleOrNull()
            val callableSymbol = getTopLevelCallableSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetFunctionName),
            ).singleOrNull()

            assertEquals(directives.expectedFileSymbolName, fileSymbol.name)
            assertEquals(directives.expectedFileSymbolPackage, fileSymbol.packageFqName.asString())

            assertNotNull(packageSymbol, "包符号应能从公开 Analysis API 恢复。")
            assertEquals(directives.expectedPackageSymbolFqName, packageSymbol!!.fqName.asString())

            assertNotNull(classLikeSymbol, "顶层 class-like 符号查询失败。")
            assertEquals(directives.expectedClassId, classLikeSymbol!!.classId.asString().replace('/', '.'))

            assertNotNull(callableSymbol, "顶层 callable 符号查询失败。")
            assertEquals(
                directives.expectedCallableId,
                callableSymbol!!.callableId?.asSingleFqName()?.asString()?.replace('/', '.'),
            )
        }
    }
}
