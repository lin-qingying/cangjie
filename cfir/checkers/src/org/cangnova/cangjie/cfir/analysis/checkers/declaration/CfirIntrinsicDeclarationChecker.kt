package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.containingExtend
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.name.Name

/**
 * `@Intrinsic` 的函数声明约束。
 *
 * 官方 parser 在函数声明解析完成后检查两个事实：intrinsic 函数必须位于顶层，
 * 且不能拥有函数体。class 等非函数声明不会进入这两个专用检查；target checker
 * 因此不应代替本 owner 产生通用的注解目标诊断。
 */
object CfirIntrinsicDeclarationChecker : CfirFunctionChecker() {
    private val allowedIntrinsicPackages = setOf(
        "std.core",
        "std.sync",
        "std.math",
        "std.overflow",
        "std.runtime",
        "std.net",
        "std.reflect",
        "std.unittest.mock.internal",
    )

    private const val headlessIntrinsicName = "getTypeForTypeParameter"

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.INTRINSIC)) return
        val function = declaration as? CfirNamedFunction ?: return

        val packageName = context.containingFileSymbol?.cfir?.packageDirective?.packageFqName?.asString()
        if (function.name.asString() != headlessIntrinsicName &&
            packageName != null && packageName !in allowedIntrinsicPackages
        ) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.INVALID_INTRINSIC_DECL,
                a = function.name.asString(),
                b = packageName,
            )
        }

        val isTopLevel = !function.isLocal &&
            function.dispatchReceiverType == null &&
            function.containingClassForStaticMemberAttr == null &&
            function.containingExtend == null
        if (!isTopLevel) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL,
            )
        }

        function.body?.let { body ->
            reporter.reportOn(
                source = body.source ?: function.source,
                factory = CfirErrors.INTRINSIC_FUNCTION_CANNOT_HAVE_BODY,
            )
        }
    }
}

/**
 * 官方 parser 在同一源文件的 intrinsic 声明表中按 identifier 去重。
 * 这是文件级事实，不能在单个 declaration checker 中用全局可变集合猜测，
 * 否则会把不同文件/不同 session 的声明错误地合并。
 */
object CfirIntrinsicDuplicateChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFile) {
        val firstByName = linkedMapOf<Name, CfirNamedFunction>()

        fun visit(current: org.cangnova.cangjie.cfir.declarations.CfirDeclaration) {
            if (current is CfirNamedFunction &&
                current.hasBuiltinAnnotation(BuiltInAnnotationKind.INTRINSIC)
            ) {
                val previous = firstByName.putIfAbsent(current.name, current)
                if (previous != null) {
                    reporter.reportOn(
                        source = current.source,
                        factory = CfirErrors.INTRINSIC_FUNCTION_DUPLICATED,
                        a = current.name.asString(),
                    )
                }
            }

            when (current) {
                is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ->
                    current.declarations.forEach(::visit)
                is org.cangnova.cangjie.cfir.declarations.CfirExtend ->
                    current.declarations.forEach(::visit)
                else -> Unit
            }
        }

        declaration.declarations.forEach(::visit)
    }
}
