package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** 诊断尚未观察、交给外部 listener，或由独立事务持有。 */
enum class CangJieLightDiagnosticOwnership { UNOBSERVED, EXTERNAL_LISTENER, BUFFERED }

/**
 * 同一次完整文件解析得到的树与冻结文法事实。
 * 仅 parser 可以构造此结果，raw 构建不能为无来源的裸树补造文件种类或 CJMP 角色。
 */
class ParsedCangJieLightFile internal constructor(
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    val fileFacts: ParsingFileFacts,
    /** 与tree同根、同次冻结；键是文件内真实AT/ATEXCL token坐标，不用于推导CFIR owner。 */
    val macroFacts: Map<Int, MacroParsingFacts>,
    /** 容器关键字的局部偏移与该容器完成解析时的直接成员快照。 */
    val cjmpNominalFacts: Map<Int, CjmpNominalSyntaxFacts>,
    /** 声明语法锚点在同次解析根内的局部偏移与角色检查事实。 */
    val declarationRoleChecks: Map<Int, CjmpDeclarationRoleCheck>,
    /** 解析时实际发生的候选检查；折叠或不完整输入以未知事件保留。 */
    val roleEvents: List<CjmpParsedRoleEvent>,
    val cjmpContext: CjmpParsingContext,
    val isCompleteFile: Boolean,
    /** 原解析入口接收的模块名；显式 package 可以在解析时覆盖结果。 */
    val inputLanguageModuleName: String,
    val diagnosticOwnership: CangJieLightDiagnosticOwnership,
    val sourceContext: CjParserSourceContext,
    val declarationPrefixes: Map<Int, CjMacroDeclarationPrefix>,
    val declarationModifiers: Map<Int, CjParsedDeclarationModifiers>,
    val lexicalInput: CjLexicalSourceInput,
    val sourceTokens: Map<Int, CjSourceTokenId>,
    val diagnosticPublication: CjParsingDiagnosticPublication,
    /** 完整文件实际 producer 的根输出，不属于任何父 parser 的入口区域。 */
    val diagnosticScope: CjParsingDiagnosticScope,
    /** 本根的完整调用配置，重用树时必须同时核验，不能只比较 CJMP 模式。 */
    val settings: CjParserSettings,
    private val verifyTokenStream: () -> Unit,
) {
    /** 提交诊断或领取消费凭证前校验本代树文本，不把行映射一致当作冻结输入一致。 */
    fun requireConsistentInput() {
        require(tree.toString(tree.root).toString() ==
            sourceContext.snapshot.text.substring(sourceContext.inputStart, sourceContext.inputEnd)) {
            "A parsed LightTree must retain its exact frozen source input."
        }
        require(lexicalInput.source === sourceContext.snapshot)
        verifyTokenStream()
    }
}

/** 同一次解析的树与尚未向外发布的诊断，由调用方决定提交时机。 */
class CangJieLightParseResult internal constructor(
    val file: ParsedCangJieLightFile,
    diagnostics: List<ParsingDiagnostic>,
) {
    val diagnostics: List<ParsingDiagnostic> = Collections.unmodifiableList(diagnostics.toList())
    private val consumed = AtomicBoolean(false)

    /** 一次解析事务只能交给一个构建调用，避免重复发布同一批诊断。 */
    fun claimForConstruction() {
        file.requireConsistentInput()
        check(consumed.compareAndSet(false, true)) { "A buffered parse transaction has already been consumed." }
    }
}
