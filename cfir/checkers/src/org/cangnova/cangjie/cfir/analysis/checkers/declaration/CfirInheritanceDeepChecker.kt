package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name

/**
 * 继承深层检查器（InheritanceDeep 分组）
 *
 * 对齐 C++ InheritanceChecker/ 目录：
 * - CANNOT_INHERIT_SEALED: sealed 类只能在同包中被继承
 * - INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC: 抽象类 static 成员未实现
 * - INVALID_MEMBER_VISIBILITY_IN_CLASS: 成员可见性不能比所在类更宽松
 *
 * 注册为 classLikeCheckers
 */
object CfirInheritanceDeepChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass) return
        checkSealedInheritanceScope(declaration)
        checkAbstractClassStaticUnimplemented(declaration)
        checkMemberVisibilityNotWiderThanClass(declaration)
    }

    /**
     * sealed 类只能在同一个包中被继承。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSealedInheritanceScope(classDecl: CfirClass) {
        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue

            if (superDecl.status.isSealed) {
                val superPackage = superClassId.packageFqName
                val currentPackage = classDecl.symbol.classId.packageFqName
                if (superPackage != currentPackage) {
                    reporter.reportOn(
                        source = superTypeRef.source ?: classDecl.source,
                        factory = CfirErrors.CANNOT_INHERIT_SEALED,
                        a = "class",
                        b = classDecl.name.asString(),
                        c = "sealed class",
                        d = superDecl.name,
                    )
                }
            }
        }
    }

    /**
     * 继承抽象类时，父类的 static 抽象函数必须被实现。
     *
     * 对齐 C++ DiagKind::sema_inherit_abstract_class_static_unimplement_func
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAbstractClassStaticUnimplemented(classDecl: CfirClass) {
        if (classDecl.status.isAbstract) return // 抽象类本身不需要实现

        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue
            if (!superDecl.status.isAbstract) continue

            // 查找父类中的 static abstract 函数
            for (superMember in superDecl.declarations) {
                if (superMember !is CfirNamedFunction) continue
                if (!superMember.status.isStatic || !superMember.status.isAbstract) continue

                // 检查子类是否实现了该 static 函数
                val implemented = classDecl.declarations.any { member ->
                    member is CfirNamedFunction &&
                        member.status.isStatic &&
                        member.name == superMember.name &&
                        member.body != null
                }
                if (!implemented) {
                    reporter.reportOn(
                        source = classDecl.source,
                        factory = CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC,
                        a = classDecl.name,
                        b = "static function",
                        c = superMember.name,
                    )
                }
            }
        }
    }

    /**
     * 成员可见性不能比所在类更宽松。
     *
     * 对齐 C++ DiagKind::sema_invalid_member_visibility_in_class:
     * 例如 private class 的成员不能是 public。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberVisibilityNotWiderThanClass(classDecl: CfirClass) {
        val classVisibility = classDecl.status.visibility
        if (classVisibility == Visibilities.Public) return // public class 无此限制

        for (member in classDecl.declarations) {
            val memberVisibility = when (member) {
                is CfirNamedFunction -> member.status.visibility
                is CfirProperty -> member.status.visibility
                else -> continue
            }
            if (memberVisibility == Visibilities.Public && classVisibility != Visibilities.Public) {
                reporter.reportOn(
                    source = member.source ?: classDecl.source,
                    factory = CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS,
                    a = memberVisibility.externalDisplayName,
                    b = classVisibility.externalDisplayName,
                )
            }
        }
    }
}
