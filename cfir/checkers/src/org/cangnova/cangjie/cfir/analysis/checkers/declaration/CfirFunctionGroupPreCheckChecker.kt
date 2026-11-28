package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.diagnostics.withRelatedInformation
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckCandidate
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckFunctionGroup
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckFunctionKind
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckScopeIdentity

/**
 * 官方过滤后名字组的 macro/enum/FFI/static 专用预检查。
 *
 * 各判据使用同一组而非物理 owner.declarations；参数 IDENTICAL 仍由重定义 collector
 * 和 resolve 层 helper 负责。函数自身 checker 不再跨局部作用域借祖先查找同名兄弟。
 */
object CfirFunctionGroupPreCheckChecker : CfirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (declaration !is CfirFile && declaration !is CfirClassLikeDeclaration && declaration !is CfirExtend) return
        val input = context.declarationPreCheckInputFor(declaration)
        for (group in input.functionGroups) {
            checkMacros(group, declaration)
            checkEnumConstructors(group, declaration)
            checkForeignNames(group, declaration)
            checkStaticFunctions(group, declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMacros(group: CfirDeclarationPreCheckFunctionGroup, owner: CfirDeclaration) {
        val macros = group.candidates.filter {
            it.functionKind == CfirDeclarationPreCheckFunctionKind.MACRO && !it.origin.generated
        }
        // 正式宏定义的 SDK 可观察规则：一个 attribute 与一个 non-attribute 宏可以共用名字。
        // >=3 个原宏定义的 native lowering/去重边界须独立验收，不把 MACRO_DECL 数量当 FUNC_DECL 数量。
        if (macros.size != 2) return
        val declarations = macros.map { it.declaration as CfirMacroDeclaration }
        if (declarations.any { it.body == null || it.valueParameters.isEmpty() }) return
        if (declarations[0].valueParameters.size != declarations[1].valueParameters.size) return
        for (candidate in macros) {
            if (!belongsToCheckedOwner(candidate, owner)) continue
            val macro = candidate.declaration as CfirMacroDeclaration
            reporter.reportOn(macro.macroNameDiagnosticSource(), CfirErrors.EXPAND_MACRO_REDEFINITION, macro.name.asString())
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEnumConstructors(group: CfirDeclarationPreCheckFunctionGroup, owner: CfirDeclaration) {
        val enum = owner as? CfirEnum ?: return
        check(group.names.scope == CfirDeclarationPreCheckScopeIdentity.Member(enum.symbol)) {
            "Enum PreCheck must use its logical declaration scope"
        }
        // native 的 enum.constructors 不随普通 member 合并；名字冲突只读取当前枚举实际拥有的构造项。
        val ownedConstructors = enum.declarations.filterIsInstance<CfirEnumConstructor>()
        val constructors = buildMap {
            // native ctorNames.emplace 保留同名构造项序列中的首项，related information 使用同一来源。
            for (constructor in ownedConstructors) putIfAbsent(constructor.name, constructor)
        }
        val retainedConstructorSymbols = group.candidates.mapNotNullTo(hashSetOf()) {
            (it.declaration as? CfirEnumConstructor)?.symbol
        }
        val seenArities = hashSetOf<Int>()
        // 构造项按原 enum 的声明序列逆向访问，不依赖 common/specific member 合并后的布局。
        for (constructor in ownedConstructors.asReversed()) {
            if (constructor.symbol !in retainedConstructorSymbols) continue
            if (!seenArities.add(constructor.valueParameters.size)) {
                reporter.reportOn(constructor.source, CfirErrors.DUPLICATED_ITEM_IN_ENUM, constructor.name, enum.name)
            }
        }
        for (candidate in group.candidates.asReversed()) {
            val declaration = candidate.declaration
            if (declaration is CfirEnumConstructor) continue
            check(candidate.logicalOwner?.symbol == enum.symbol) {
                "Enum member PreCheck must retain its logical owner"
            }
            val constructor = constructors[group.names.name] ?: continue
            val reportContext = context.withRelatedInformation(
                listOf(constructor.relatedInformation("'${constructor.name.asString()}' is previously declared here")),
            )
            reporter.reportOn(declaration.source, CfirErrors.REDECLARATION, listOf(group.names.name.asString()), context = reportContext)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkForeignNames(group: CfirDeclarationPreCheckFunctionGroup, owner: CfirDeclaration) {
        val native = group.candidates.filter { it.attributes.isForeign || it.attributes.isC }
        val first = native.firstOrNull() ?: return
        for (candidate in native.drop(1)) {
            if (candidate.functionKind == CfirDeclarationPreCheckFunctionKind.CONSTRUCTOR ||
                !belongsToCheckedOwner(candidate, owner)
            ) continue
            val information = first.declaration.relatedInformation("'${group.names.name.asString()}' is previously declared here")
            val reportContext = context.forDiagnosticSourceOwner(candidate.declaration.symbol, candidate.declaration.source)
                .withRelatedInformation(listOf(information))
            reporter.reportOn(candidate.declaration.source, CfirErrors.REDECLARATION, listOf(group.names.name.asString()), context = reportContext)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticFunctions(group: CfirDeclarationPreCheckFunctionGroup, owner: CfirDeclaration) {
        val nonConstructorFunctions = group.candidates.filter { it.functionKind != CfirDeclarationPreCheckFunctionKind.CONSTRUCTOR }
        val firstNonStatic = nonConstructorFunctions.firstOrNull { !it.attributes.isStatic } ?: return
        for (candidate in nonConstructorFunctions.filter { it.attributes.isStatic }) {
            if (!belongsToCheckedOwner(candidate, owner)) continue
            val information = firstNonStatic.declaration.relatedInformation("conflict with the declaration")
            val reportContext = context.forDiagnosticSourceOwner(candidate.declaration.symbol, candidate.declaration.source)
                .withRelatedInformation(listOf(information))
            reporter.reportOn(candidate.declaration.source, CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS, group.names.name, context = reportContext)
        }
    }

    /** 包级组跨文件共享，单文件只报告本文件；成员组的 common CJO 诊断保留原声明来源。 */
    private fun belongsToCheckedOwner(candidate: CfirDeclarationPreCheckCandidate, owner: CfirDeclaration): Boolean =
        owner !is CfirFile || owner.declarations.any { it.symbol === candidate.declaration.symbol }
}
