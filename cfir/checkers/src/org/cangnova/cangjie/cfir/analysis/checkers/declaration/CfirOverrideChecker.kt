package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 覆盖（Override）检查器。
 * 基于仓颉官方规则：
 * - 隐式覆盖：无 override 关键字，签名匹配自动覆盖
 * - 非抽象 class 需 open 才可被继承
 * - 覆盖条件：参数类型完全相同 + 参数名完全相同
 * - 返回类型：子类返回类型必须是父类返回类型的子类型（协变）
 * - 可见性：覆盖函数可见性 >= 被覆盖函数可见性
 *
 * 对齐官方编译器 `StructInheritanceChecker.cpp#CheckImplementationRelation`。
 */
object CfirOverrideChecker : CfirDeclarationChecker<CfirClass>(CheckerDispatchKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClass) {
        // 接口无需覆盖检查
        if (declaration.classKind == CfirClassKind.INTERFACE) return

        // 检查父类是否 open
        checkParentIsOpen(declaration)

        // 收集自身函数
        val ownFunctions = declaration.declarations.filterIsInstance<CfirFunction>()
        if (ownFunctions.isEmpty()) return

        // 收集父类函数
        val parentFunctions = collectParentFunctions(declaration, context)
        if (parentFunctions.isEmpty()) return

        // 对签名匹配的对进行覆盖规则检查
        for (ownFunc in ownFunctions) {
            val overridden = findOverridden(ownFunc, parentFunctions)
            for (parent in overridden) {
                checkReturnTypeCovariance(ownFunc, parent)
                checkVisibilityNotReduced(ownFunc, parent)
            }
        }
    }

    /**
     * 检查父类是否标记为 open（仅对 class 的 class 父类型检查）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParentIsOpen(declaration: CfirClass) {
        for (superRef in declaration.superTypeRefs) {
            val superType = (superRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            if (superType !is ConeClassLikeType) continue

            val superSymbol = context.session.symbolProvider
                .getClassLikeSymbolByClassId(superType.lookupTag.classId) ?: continue
            if (!superSymbol.isBound) continue
            val superClass = superSymbol.cfir as? CfirClass ?: continue

            // 只有 class 类型需要 open 检查（interface 不需要）
            if (superClass.classKind != CfirClassKind.CLASS) continue

            if (!superClass.status.isOpen && !superClass.status.isAbstract) {
                val source = superRef.source as? AbstractCjSourceElement ?: continue
                reporter.reportOn(source, CfirErrors.CLASS_NOT_OPEN_FOR_INHERITANCE, superClass.name)
            }
        }
    }

    /**
     * 从父类型中收集所有非 private、非构造函数的函数成员。
     */
    private fun collectParentFunctions(
        declaration: CfirClass,
        context: CheckerContext,
    ): List<CfirFunction> {
        val result = mutableListOf<CfirFunction>()
        for (superRef in declaration.superTypeRefs) {
            val superType = (superRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType || superType !is ConeClassLikeType) continue

            val superSymbol = context.session.symbolProvider
                .getClassLikeSymbolByClassId(superType.lookupTag.classId) ?: continue
            if (!superSymbol.isBound) continue
            val superClass = superSymbol.cfir as? CfirClass ?: continue

            for (decl in superClass.declarations) {
                if (decl !is CfirFunction) continue
                val visibility = decl.status.visibility
                if (Visibilities.isPrivate(visibility)) continue
                result.add(decl)
            }
        }
        return result
    }

    /**
     * 查找与 ownFunc 签名匹配的父类函数。
     * 匹配条件：名称相同 + 参数个数相同 + 参数类型相同 + 参数名称相同。
     */
    private fun findOverridden(
        ownFunc: CfirFunction,
        parentFunctions: List<CfirFunction>,
    ): List<CfirFunction> {
        return parentFunctions.filter { parent ->
            parent.name == ownFunc.name &&
                parent.valueParameters.size == ownFunc.valueParameters.size &&
                parametersMatch(ownFunc.valueParameters, parent.valueParameters)
        }
    }

    private fun parametersMatch(
        own: List<CfirValueParameter>,
        parent: List<CfirValueParameter>,
    ): Boolean {
        for (i in own.indices) {
            // 参数名称必须相同
            if (own[i].name != parent[i].name) return false
            // 参数类型必须相同
            val ownType = (own[i].returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
            val parentType = (parent[i].returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
            if (ownType != parentType) return false
        }
        return true
    }

    /**
     * 检查返回类型协变：子类返回类型必须是父类返回类型的子类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReturnTypeCovariance(
        ownFunc: CfirFunction,
        parentFunc: CfirFunction,
    ) {
        val ownReturnType = (ownFunc.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val parentReturnType = (parentFunc.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (ownReturnType is ConeErrorType || parentReturnType is ConeErrorType) return

        if (!CfirTypeCheckUtils.isSubtypeOf(ownReturnType, parentReturnType)) {
            val source = ownFunc.source as? AbstractCjSourceElement ?: return
            reporter.reportOn(
                source,
                CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH,
                ownReturnType,
                parentReturnType,
                parentFunc.name,
            )
        }
    }

    /**
     * 检查可见性不能缩小：覆盖函数可见性 >= 被覆盖函数可见性。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVisibilityNotReduced(
        ownFunc: CfirFunction,
        parentFunc: CfirFunction,
    ) {
        val ownVisibility = ownFunc.status.visibility
        val parentVisibility = parentFunc.status.visibility

        val comparison = Visibilities.compare(ownVisibility, parentVisibility)
        // comparison < 0 means own is less visible
        if (comparison != null && comparison < 0) {
            val source = ownFunc.source as? AbstractCjSourceElement ?: return
            reporter.reportOn(source, CfirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER, parentFunc.name)
        }
    }
}
