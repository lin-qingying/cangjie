package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

/**
 * CFIR 声明级检查器的基础抽象。
 *
 * 每个具体 checker 通过类型参数限定自己接收的声明种类，并在 checker registry
 * 分发到对应 CFIR 声明时执行诊断逻辑。
 */
abstract class CfirDeclarationChecker<D : CfirDeclaration> {
    /**
     * 该 checker 是否检查"实现的完备性"——即依赖函数体 / 初始化器 / 访问器**实现的存在性**。
     *
     * 声明文件（`.cj.d`）中的声明**没有体**，因此这类检查在声明文件上整体跳过，
     * 对齐官方 Sema 的实现完备性豁免（`InitializationChecker` / `StructInheritanceChecker` /
     * `DeclAttributeChecker::CheckAttributesForPropAndFuncDeclInClass` 的 `opts.compileCjd` 提前返回）。
     *
     * 默认 `false`：新增 checker **默认在所有模式下参与**，其作者无需知道 `.cj.d` 的存在；
     * 只有"缺了实现就报错"的检查才需要覆写为 `true`（如缺初始化、未实现接口成员、入口缺失）。
     *
     * 过滤发生在 [DeclarationCheckersDiagnosticComponent][org.cangnova.cangjie.cfir.analysis.checkers.declaration]
     * 的统一分派处（生成代码，见 `checkers-component-generator`），不在各 checker 内部。
     */
    open val requiresImplementation: Boolean get() = false

    /**
     * 对单个声明执行诊断检查。
     *
     * context 提供当前文件、session、作用域和所有者链信息；reporter 负责把检查结果
     * 统一提交到 CFIR 诊断管线。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(declaration: D)
}

