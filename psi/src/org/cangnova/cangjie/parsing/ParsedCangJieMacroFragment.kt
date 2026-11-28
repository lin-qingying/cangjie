package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** 同根 parser 的直接 roots 与完整结果；root 引用属于返回的这棵 LightTree。 */
class ParsedCangJieMacroFragment internal constructor(
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    roots: List<LighterASTNode>,
    val fileFacts: ParsingFileFacts,
    val parsingInfo: CjMacroFragmentParseInfo,
    macroFacts: Map<Int, MacroParsingFacts>,
    nominalFacts: Map<Int, CjmpNominalSyntaxFacts>,
    roleChecks: Map<Int, CjmpDeclarationRoleCheck>,
    roleEvents: List<CjmpParsedRoleEvent>,
    /** 与语法树同次解析的完整入口，不能由当前 owner 或解析后的角色反推。 */
    val inputSeed: CjParserSeed,
    /** 与语法树同次解析的精确输入；后续文本修改不能改变该事务。 */
    val inputText: String,
    /** 明确诊断是否已交给外部，或仍由事务持有。 */
    val diagnosticOwnership: CangJieLightDiagnosticOwnership,
    val sourceContext: CjParserSourceContext,
    declarationPrefixes: Map<Int, CjMacroDeclarationPrefix>,
    declarationModifiers: Map<Int, CjParsedDeclarationModifiers>,
    val lexicalInput: CjLexicalSourceInput,
    val diagnosticPublication: CjParsingDiagnosticPublication,
    /** 本次根的发布时钟；inputSeed 中的父诊断区域保持原输入语义。 */
    val diagnosticScope: CjParsingDiagnosticScope,
    private val verifyTokenStream: () -> Unit,
) {
    /** 原入口对象用于父槽位身份；同文本重解析使用本次实际冻结的输入，不能重新制造来源。 */
    val actualInputSeed: CjParserSeed = inputSeed.copy(sourceContext = sourceContext, lexicalInput = lexicalInput)
    val roots: List<LighterASTNode> = Collections.unmodifiableList(roots.toList())
    val macroFacts: Map<Int, MacroParsingFacts> = Collections.unmodifiableMap(LinkedHashMap(macroFacts))
    val cjmpNominalFacts: Map<Int, CjmpNominalSyntaxFacts> = Collections.unmodifiableMap(LinkedHashMap(nominalFacts))
    val declarationRoleChecks: Map<Int, CjmpDeclarationRoleCheck> = Collections.unmodifiableMap(LinkedHashMap(roleChecks))
    val roleEvents: List<CjmpParsedRoleEvent> = Collections.unmodifiableList(roleEvents.toList())
    val declarationPrefixes: Map<Int, CjMacroDeclarationPrefix> = Collections.unmodifiableMap(LinkedHashMap(declarationPrefixes))
    val declarationModifiers: Map<Int, CjParsedDeclarationModifiers> = Collections.unmodifiableMap(LinkedHashMap(declarationModifiers))

    /** 宏树、输入切片及冻结词法来源必须属于同一实际代次。 */
    fun requireConsistentInput() {
        sourceContext.requireInput(inputText)
        require(lexicalInput.source === sourceContext.snapshot)
        require(tree.toString(tree.root).toString() == inputText) {
            "A parsed macro LightTree must retain its exact frozen source input."
        }
        verifyTokenStream()
    }
}

/** 宏片段的同次解析结果及未发布诊断；两者只能作为一组领取一次。 */
class CangJieLightMacroParseResult internal constructor(
    private val fragment: ParsedCangJieMacroFragment,
    diagnostics: List<ParsingDiagnostic>,
) {
    private val diagnostics: List<ParsingDiagnostic> = Collections.unmodifiableList(diagnostics.toList())
    private val consumed = AtomicBoolean(false)

    /** 领取同一代次的片段与诊断；调用方负责完成构建和诊断提交。 */
    fun <R> consume(action: (ParsedCangJieMacroFragment, List<ParsingDiagnostic>) -> R): R {
        fragment.requireConsistentInput()
        check(consumed.compareAndSet(false, true)) { "A buffered macro parse transaction has already been consumed." }
        val result = action(fragment, diagnostics)
        fragment.requireConsistentInput()
        return result
    }

    /**
     * 以调用方明确提供的目标入口角色重解析原始文本；旧诊断随旧代次一起失效。
     * 源码种类、文法入口、前缀及局部语境继承原 seed，不能从输出角色反推。
     */
    fun reparse(
        targetContext: CjmpParsingContext.Compilation,
        entryFileRole: CjmpFileRole,
    ): CangJieLightMacroParseResult {
        require(targetContext.initialFileRole == entryFileRole) { "Macro reparse requires one consistent target entry role." }
        return consume { original, _ ->
            val seed = original.actualInputSeed.copy(cjmpContext = targetContext, fileRole = entryFileRole, diagnosticScope = null)
            val builder = CangJieLightParser.createBuilder(original.inputText)
            CangJieLightParser.parseMacroCodeFragmentBuffered(builder, seed)
        }
    }

    /** 确保最终事务属于调用方提供的语境和入口角色；一致时保留原代诊断。 */
    fun forContext(
        targetContext: CjmpParsingContext.Compilation,
        entryFileRole: CjmpFileRole,
    ): CangJieLightMacroParseResult {
        require(targetContext.initialFileRole == entryFileRole) { "Macro construction requires one consistent target entry role." }
        return consume { original, oldDiagnostics ->
            val originalContext = original.inputSeed.cjmpContext as? CjmpParsingContext.Compilation
            if (original.inputSeed.fileRole == entryFileRole &&
                originalContext?.copy(initialFileRole = entryFileRole) == targetContext
            ) {
                CangJieLightMacroParseResult(original, oldDiagnostics)
            } else {
                val seed = original.actualInputSeed.copy(cjmpContext = targetContext, fileRole = entryFileRole, diagnosticScope = null)
                val builder = CangJieLightParser.createBuilder(original.inputText)
                CangJieLightParser.parseMacroCodeFragmentBuffered(builder, seed)
            }
        }
    }
}

/** roots 不是声明/表达式白名单；foreign 等真实语法根也必须保留。 */
internal fun IElementType.isMacroFragmentRootType(): Boolean =
    this !is CjToken && this !in CjTokens.ALL_WHITESPACE_TOKENS && this !in CjTokens.COMMENTS &&
        this != CjTokens.UNKNOWN_TOKEN_START &&
        this != TokenType.BAD_CHARACTER && this != TokenType.ERROR_ELEMENT &&
        this !is CjParsingErrorElementType

/** 只走固定的 fragment→contents→roots 两级结构；不搜索任何语法后代。 */
internal fun FlyweightCapableTreeStructure<LighterASTNode>.macroFragmentRoots(): List<LighterASTNode> {
    fun selectDirectChildren(node: LighterASTNode, predicate: (LighterASTNode) -> Boolean): List<LighterASTNode> {
        val ref = Ref<Array<LighterASTNode>>()
        val count = getChildren(node, ref)
        val children = ref.get() ?: LighterASTNode.EMPTY_ARRAY
        return try {
            // SDK 会在 disposeChildren 回收 token 对象；必须在借用期间排除词元与诊断载体。
            (0 until count).map { children[it] }.filter(predicate)
        } finally { disposeChildren(children, count) }
    }
    val contents = selectDirectChildren(root) { it.tokenType == CjNodeTypes.MACRO_FRAGMENT_CONTENTS }.single()
    return selectDirectChildren(contents) { it.tokenType.isMacroFragmentRootType() }
}
