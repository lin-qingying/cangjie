package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * typealias 缩写类型测试。
 *
 * 这里固定按公开 top-level symbol lookup 恢复 callable，再观察它的返回类型：
 * 这样 source / library binary 两条生成路径都能复用同一份 testData。
 */
abstract class AbstractAbbreviatedTypeTest : AbstractTypeTest() {
    /**
     * 通过顶层 callable 的返回类型获取待观察类型。
     *
     * testData 使用目标函数名定位 callable，测试随后观察其返回类型的 abbreviation 与 fully expanded 形态。
     */
    override fun getType(
        analysisSession: CaSession,
        cjFile: CjFile,
        module: CjTestModule,
        testServices: TestServices,
    ) = with(analysisSession) {
        val directives = directivesForMainFile(cjFile, module)
        val callableSymbol = getTopLevelCallableSymbols(
            cjFile.packageFqName,
            Name.identifier(directives.targetFunctionName),
        ).singleOrNull() as? CaCallableSymbol
            ?: error("Cannot resolve callable `${directives.targetFunctionName}` for abbreviated type test.")

        callableSymbol.returnType
    }
}
