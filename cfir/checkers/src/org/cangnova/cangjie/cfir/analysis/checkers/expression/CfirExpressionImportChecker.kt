package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.resolve.AST_PACKAGE_FQ_NAME
import org.cangnova.cangjie.cfir.resolve.SYNC_PACKAGE_FQ_NAME
import org.cangnova.cangjie.cfir.resolve.importsPackageOrMember
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.FqName

/**
 * 内建表达式形式「需要先导入对应标准库包」检查器。
 *
 * 对齐官方 `sema_use_expr_without_import`：官方在 `QuoteExpr.cpp:26`、`SynchronizedExpr.cpp:27`
 * 与 `CheckAPILevel.cpp:264/280` 三处发出该诊断，语义都是「该形式所需的标准库声明不可用」
 * （判定走 `ImportManager::GetImportedDecl`，即包未被导入时查不到成员）。
 * 本 checker 承担与导入直接相关的表达式形式；`@IfAvailable` 的同类诊断由
 * [CfirIfAvailableExpressionChecker] 承担（它的判定条件含 API 等级，不属于纯导入缺失）。
 *
 * 锚点沿用官方：`quote` 锚整个表达式（官方传 `qe`），`synchronized` 锚锁对象（官方传 `*se.mutex`）。
 */
object CfirExpressionImportChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        when (expression) {
            is CfirQuoteExpression -> checkQuoteImport(expression)
            is CfirSynchronizedExpression -> checkSynchronizedImport(expression)
            else -> Unit
        }
    }

    /** `quote` 表达式需要导入 `std.ast`。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkQuoteImport(expression: CfirQuoteExpression) {
        if (isPackageImported(AST_PACKAGE_FQ_NAME)) return
        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.USE_EXPR_WITHOUT_IMPORT,
            a = AST_PACKAGE_FQ_NAME,
            b = "quote",
        )
    }

    /**
     * `synchronized` 表达式要求 `std.sync` 已参与本编译单元。
     *
     * 官方判定是 `ImportManager::GetSyncDecl` → `GetImportedDecl(SYNC_PACKAGE_NAME, "Lock")`，
     * 即「cjoManager 里已加载的 `std.sync` 包能否查到 `Lock`」——加载与否由**包级** import
     * 并集决定（同一包任一文件 `import std.sync` 即加载），而不是当前文件的 import 列表。
     * 真实项目 mariadb `src/cdbc/transaction.cj` 未 import `std.sync`，但同包 `connection.cj`
     * 导入了，cjc 编译通过；只按当前文件判定会误报。
     *
     * CFIR 侧无法廉价枚举同包兄弟文件的 import，因此采用等价的可判定证据：
     * **锁对象的静态类型本身来自 `std.sync`**（如 `Mutex` / `ReentrantMutex`）时，
     * 说明该编译单元必然已加载 `std.sync`，不报该诊断。
     * 诊断锚点沿用官方（锁对象）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSynchronizedImport(expression: CfirSynchronizedExpression) {
        if (isPackageImported(SYNC_PACKAGE_FQ_NAME)) return
        if (expression.monitor.coneTypeOrNull.isDeclaredInSyncPackage()) return
        reporter.reportOn(
            source = expression.monitor.source,
            factory = CfirErrors.USE_EXPR_WITHOUT_IMPORT,
            a = FqName("sync"),
            b = "synchronized",
        )
    }

    /** 判断类型是否声明在 `std.sync` 包内（即该编译单元已加载 `std.sync` 的可判定证据）。 */
    private fun ConeCangJieType?.isDeclaredInSyncPackage(): Boolean {
        val classId = this?.classIdOrPrimitiveClassId ?: return false
        return classId.packageFqName == SYNC_PACKAGE_FQ_NAME
    }

    /** 当前表达式所在文件是否导入了 [packageFqName] 包或其成员。 */
    context(context: CheckerContext)
    private fun isPackageImported(packageFqName: FqName): Boolean =
        context.containingFileSymbol?.cfir?.importsPackageOrMember(packageFqName) == true
}
