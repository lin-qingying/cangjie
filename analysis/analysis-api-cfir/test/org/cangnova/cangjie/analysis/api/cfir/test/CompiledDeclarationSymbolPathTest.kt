package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * compiled（库/反编译）声明在三条崩溃链所经 API 上的回归测试。
 *
 * 现场的三条栈都是「拿一个**已编译**声明的符号，符号没恢复出来，于是整段补全/提示消失」：
 * 1. 补全尾文本 → `CaConstructorSymbol.valueParameters` → 库模块拿不到 PSI 形参 →
 *    回落 `cfirSymbol.createCjValueParameters`；
 * 2. 声明返回类型查询 → `CjCallableDeclaration.returnType`；
 * 3. 缓存把「无上下文查询得到的 null」当成命中（由 `CfirThreadSafeCacheTest` 覆盖）。
 *
 * 本用例用真实的 decompiled `std.core` 声明走完 1、2 两条链：断言形参与返回类型都是
 * **可用的**符号（非错误类型、不抛异常）。它覆盖的是「compiled 声明 → 符号 API」这条
 * 生产路径本身；「compiled 声明在索引里真的缺失」需要一份刻意损坏的 `.cjo`/反编译 PSI
 * fixture，属另一类夹具，见交接文档 G6。
 */
class CompiledDeclarationSymbolPathTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/diagnostics",
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    /**
     * 库构造器的形参与库函数的返回类型都必须可恢复。
     */
    @Test
    fun compiledDeclarationSymbolPath(mainFile: CjFile) {
        val stdCore = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(mainFile.project)
            .mapNotNull { virtualFile -> PsiManager.getInstance(mainFile.project).findFile(virtualFile) }
            .filterIsInstance<CjFile>()
            .firstOrNull { file -> file.name.contains("std.core") }
        assertNotNull(stdCore, "必须能定位 decompiled std.core PSI")

        val arrayDeclaration = PsiTreeUtil
            .findChildrenOfType(stdCore!!, CjClassLikeDeclaration::class.java)
            .firstOrNull { declaration -> declaration.name == "Array" }
        assertNotNull(arrayDeclaration, "std.core 必须包含 Array 声明")

        val libraryFunction = PsiTreeUtil
            .findChildrenOfType(stdCore, CjNamedFunction::class.java)
            .firstOrNull { function -> function.name != null && function.typeReference != null }
        assertNotNull(libraryFunction, "std.core 必须包含带显式返回类型的函数")

        val report = analyzeForTest(mainFile) {
            // 直接用 decompiled PSI 取公开符号：这正是生产栈走的入口（补全渲染拿到的是
            // 库声明的 PSI，再由它构造符号），比按 ClassId 查找更贴近现场。
            val declarationSymbol = with(this) { arrayDeclaration!!.symbol }
            val classSymbol = declarationSymbol as? CaClassSymbol
            assertNotNull(classSymbol, "std.core.Array 的 PSI 必须能恢复成 class 符号")

            // CaClassSymbol 同时满足两个 memberScope 扩展的 receiver，必须收紧到单一静态类型。
            val classLike: CaClassLikeSymbol = classSymbol!!
            val constructors = with(this) { classLike.declaredMemberScope.constructors.toList() }
            assertTrue(constructors.isNotEmpty(), "std.core.Array 必须有构造器候选")

            // 符号对象不能逃出 analyze 生命周期，这里只带回稳定字符串。
            val classTypeParameterCount = classLike.typeParameters.size
            val constructorReports = constructors.map { constructor ->
                val parameters = constructor.valueParameters
                val errorParameterTypes = parameters.count { parameter ->
                    with(this) { parameter.returnType.isErrorType }
                }
                "params=${parameters.size},errorParams=$errorParameterTypes," +
                    "typeParams=${constructor.typeParameters.size}," +
                    "returnTypeError=${with(this) { constructor.returnType.isErrorType }}"
            }

            // 构造器的类型参数口径必须与所属类一致：源码路径取外层类，CFIR 路径曾取构造器自身
            // （官方禁止泛型构造器，那里恒为空），两条路径会对同一个库构造器给出不同答案。
            val typeParametersMatchOwner =
                constructors.all { constructor -> constructor.typeParameters.size == classTypeParameterCount }

            val callableReturnType = with(this) { libraryFunction!!.returnType }
            val callableReturnTypeError = callableReturnType?.let { type -> with(this) { type.isErrorType } }
            "classTypeParams=$classTypeParameterCount," +
                "typeParametersMatchOwner=$typeParametersMatchOwner," +
                "constructors=[${constructorReports.joinToString("; ")}]," +
                "callableReturnTypeMissing=${callableReturnType == null}," +
                "callableReturnTypeError=$callableReturnTypeError"
        }

        assertTrue(
            report.contains("errorParams=0"),
            "库构造器的形参类型必须全部可恢复（缺失会被渲染成 `name: ?` 或丢掉候选）：$report",
        )
        assertTrue(
            report.contains("returnTypeError=false"),
            "库构造器的返回类型不能是错误类型：$report",
        )
        assertTrue(
            report.contains("typeParametersMatchOwner=true"),
            "库构造器的类型参数必须与所属类一致（CFIR 分支曾退回构造器自身列表）：$report",
        )
        assertTrue(
            report.contains("callableReturnTypeMissing=false") && report.contains("callableReturnTypeError=false"),
            "库函数的返回类型必须可恢复：$report",
        )
    }
}
