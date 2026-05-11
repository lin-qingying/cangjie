package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.Name

/**
 * ThreadContext 继承约束检查器（InheritanceDeep 补充）
 *
 * 对齐 C++ InheritanceChecker 中 ThreadContext 相关检查:
 * - 继承 ThreadContext 的类型必须满足特定约束（必须是 open class）
 * - ThreadContext 子类型不能是 sealed/abstract 而不是 open
 *
 * 注册为 classLikeCheckers
 */
object CfirInheritanceThreadContextChecker : CfirClassLikeChecker() {
    private val THREAD_CONTEXT = Name.identifier("ThreadContext")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass) return

        for (superTypeRef in declaration.superTypeRefs) {
            val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue

            if (superClassId.shortClassName != THREAD_CONTEXT) continue

            val superSymbol = context.session.symbolProvider
                .getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue

            // ThreadContext 子类型必须是 open 的
            if (!superDecl.status.isOpen && !superDecl.status.isAbstract) {
                reporter.reportOn(
                    source = superTypeRef.source ?: declaration.source,
                    factory = CfirErrors.INHERIT_THREAD_CONTEXT_NOT_OPEN,
                    a = superDecl.name,
                )
            }

            // 继承 ThreadContext 的声明自身的合法性检查
            if (declaration.status.isSealed) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.INHERIT_THREAD_CONTEXT_INVALID,
                    a = declaration.name,
                )
            }
        }
    }
}
