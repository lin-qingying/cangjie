package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPlatformAnnotationClassIds
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroPayloadChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.name.Name

/**
 * 检查 `APILevel` / `Hide` 与声明宏、普通 custom annotation 的原始嵌套顺序。
 *
 * 官方 `PluginCustomAnnoChecker::CheckAnnoBeforeMacro` 遍历的是
 * `file.originalMacroCallNodes`：只有外层 `APILevel` / `Hide` 的直接 declaration
 * input 仍是非 `APILevel` / `Hide` 的 `MacroExpandDecl` 时才报 `HIDE_MUST_AT_END`。
 * 因此本 checker 以文件级 construction surface forest 为 owner，禁止从 final CFIR
 * 的 declaration annotation 列表推导位置关系。
 */
object CfirPlatformAnnotationMacroOrderChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val registry = context.session.macroExpansionRegistry ?: return
        val surfaces = registry.originSurfaces(declaration)
            .filterIsInstance<MacroSurfaceDecl>()
        if (surfaces.isEmpty()) return

        val forest = MacroCallForestBuilder.build(surfaces)
        for (root in forest.roots) {
            checkNode(root)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNode(node: MacroCallNode) {
        val outerName = node.surface.qualifiedName?.shortName()
        if (outerName != null && outerName in platformOrderAnnotationNames) {
            val offendingChild = node.childEdges
                .asSequence()
                .filter { edge -> edge.channel == MacroPayloadChannel.INPUT }
                .map { edge -> edge.child }
                .filter { child -> child.surface is MacroSurfaceDecl }
                .firstOrNull { child ->
                    val childName = child.surface.qualifiedName?.shortName()
                    childName == null || childName !in platformOrderAnnotationNames
                }
            if (offendingChild != null) {
                reporter.reportOn(
                    source = node.surface.sourceRange?.source,
                    factory = CfirErrors.HIDE_MUST_AT_END,
                    a = outerName.asString(),
                )
                return
            }
        }

        for (child in node.children) {
            checkNode(child)
        }
    }
}

private val platformOrderAnnotationNames: Set<Name> = setOf(
    CfirPlatformAnnotationClassIds.API_LEVEL.shortClassName,
    CfirPlatformAnnotationClassIds.HIDE.shortClassName,
)
