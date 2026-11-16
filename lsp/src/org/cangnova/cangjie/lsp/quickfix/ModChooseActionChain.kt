package org.cangnova.cangjie.lsp.quickfix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModChooseAction
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandAction
import com.intellij.modcommand.Presentation
import com.intellij.openapi.progress.ProcessCanceledException
import java.util.logging.Level
import java.util.logging.Logger

/** 展开嵌套 ModChooseAction 后到达的一条终结动作路径。 */
internal data class ModChooseActionChain(
    val steps: List<Step>,
    val leaf: Leaf,
) {
    data class Step(
        val action: ModCommandAction,
        val presentation: Presentation,
        val command: ModChooseAction,
    )

    data class Leaf(
        val action: ModCommandAction,
        val presentation: Presentation,
        val command: ModCommand,
    )
}

internal fun ModChooseActionChain.combinedPresentationNames(): String =
    (steps.map { it.presentation.name } + leaf.presentation.name).joinToString(" → ")

/**
 * LSP 没有嵌套 chooser 控件，因此像 Kotlin LSP 一样把每个选择分支展开为独立 CodeAction。
 */
internal fun ModCommandAction.flattenChoiceActions(context: ActionContext): List<ModChooseActionChain> =
    flattenChoiceActionsImpl(context, emptyList()).orEmpty()

private fun ModCommandAction.flattenChoiceActionsImpl(
    context: ActionContext,
    steps: List<ModChooseActionChain.Step>,
): List<ModChooseActionChain>? {
    val presentation = try {
        getPresentation(context)
    } catch (cancelled: ProcessCanceledException) {
        throw cancelled
    } catch (failure: Exception) {
        LOG.log(Level.WARNING, "Could not get ModCommand presentation", failure)
        return null
    } ?: return emptyList()

    val command = try {
        perform(context)
    } catch (cancelled: ProcessCanceledException) {
        throw cancelled
    } catch (failure: Exception) {
        LOG.log(Level.WARNING, "Could not perform ModCommand action", failure)
        return null
    }

    return when (command) {
        is ModChooseAction -> {
            val nextSteps = steps + ModChooseActionChain.Step(this, presentation, command)
            command.actions.flatMap { child ->
                child.flattenChoiceActionsImpl(context, nextSteps) ?: return null
            }
        }
        else -> listOf(ModChooseActionChain(steps, ModChooseActionChain.Leaf(this, presentation, command)))
    }
}

/** 平台明确允许默认选项时，LSP 使用默认选择继续生成编辑。 */
internal fun ModCommand.resolveLspDefaultOptions(): ModCommand? {
    return when (this) {
        is com.intellij.modcommand.ModEditOptions<*> -> {
            if (!canUseDefaults()) return null
            nextCommand().apply(containerSupplier().get()).resolveLspDefaultOptions()
        }
        is com.intellij.modcommand.ModCompositeCommand -> {
            val resolved = commands.map { child -> child.resolveLspDefaultOptions() ?: return null }
            com.intellij.modcommand.ModCompositeCommand(resolved)
        }
        else -> this
    }
}

private val LOG: Logger = Logger.getLogger("org.cangnova.cangjie.lsp.quickfix.ModChooseActionChain")
