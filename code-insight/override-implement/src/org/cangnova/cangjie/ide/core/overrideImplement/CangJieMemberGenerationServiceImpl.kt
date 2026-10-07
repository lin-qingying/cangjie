package org.cangnova.cangjie.ide.core.overrideImplement

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.codeinsight.api.members.CangJieGeneratableMember
import org.cangnova.cangjie.codeinsight.api.members.CangJieMemberGenerationService
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 成员生成窄接口的适配实现。
 *
 * 它把内部处理器的「收集」结果翻译成接口类型：内部对象带着 chooser 语义与符号指针，
 * 接口只保留**名字**与**渲染后的声明文本**。
 *
 * 名字经符号恢复得到，不从渲染文本里截取：从 `func greet(name: String)` 这类文本里
 * 猜名字，在参数、泛型、命名参数都出现后会立刻出错。
 */
@OptIn(CaExperimentalApi::class)
internal class CangJieMemberGenerationServiceImpl : CangJieMemberGenerationService {
    /**
     * 复用既有处理器收集成员。
     *
     * 调的是**内层**的 `collectMembersToGenerate`，不是带进度窗口的那一层：
     * 后者会开同步模态进度（`runProcessWithProgressSynchronously`），那是给 UI 动作用的。
     * 非 UI 消费者在后台线程上掉进那里会直接死锁——
     * 实测报 `Can't invokeAndWait from WT to EDT: probably leads to deadlock`。
     * 内层带 `@RequiresBackgroundThread`，正是这里需要的前提。
     */
    private val handler = object : ImplementMembersHandler() {
        /**
         * 暴露受保护的后台线程收集入口。
         */
        fun collect(typeStatement: CjTypeStatement) = collectMembersToGenerate(typeStatement)
    }

    override fun collectUnimplementedMembers(typeStatement: CjTypeStatement): List<CangJieGeneratableMember> =
        analyze(typeStatement) {
            handler.collect(typeStatement).map { chooser ->
                val symbol = chooser.symbolPointer.restoreSymbol(this)
                GeneratableMember(
                    name = (symbol as? CaNamedSymbol)?.name?.asString() ?: chooser.text,
                    renderedText = chooser.text,
                )
            }
        }
}

/**
 * 接口的标准实现。
 */
private data class GeneratableMember(
    override val name: String,
    override val renderedText: String,
) : CangJieGeneratableMember
