package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * 通过全限定名查询 symbol 的抽象测试。
 *
 * 测试读取 class-like 与 callable 全限定名指令，通过公开 lookup API 收集 symbol 并复用公共 symbol 断言。
 */
abstract class AbstractSymbolByFqNameTest : AbstractSymbolTest() {
    /**
     * 按 `TARGET_CLASS_FQ_NAME` 和 `TARGET_CALLABLE_FQ_NAME` 收集 symbol。
     *
     * class-like 使用 `getClassLikeSymbol(ClassId.topLevel(...))`，callable 使用包名与短名查询顶层 callable 集合。
     */
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData {
        val module = testServices.cjTestModuleStructure.requireModuleByFile(cjFile)
        val directives = directivesForMainFile(cjFile, module)
        return SymbolsData(
            buildList {
                directives[SymbolTestDirectives.TARGET_CLASS_FQ_NAME].mapTo(this) { className ->
                    getClassLikeSymbol(ClassId.topLevel(FqName.fromString(className)))
                        ?: error("Class-like symbol '$className' was not found")
                }
                directives[SymbolTestDirectives.TARGET_CALLABLE_FQ_NAME].flatMapTo(this) { callableName ->
                    val fqName = FqName.fromString(callableName)
                    getTopLevelCallableSymbols(fqName.parent(), Name.identifier(fqName.shortName().asString()))
                }
            },
        )
    }
}
