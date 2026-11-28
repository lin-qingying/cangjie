package org.cangnova.cangjie.cfir.builder

import java.util.IdentityHashMap
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceSpan
import org.cangnova.cangjie.cfir.resolve.providers.macro.macroSourceTreeIdentity
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.source.withRange

/**
 * raw builder 的 source 创建入口。宏重解析实现同时处理 AST source 与 token 来源，
 * 防止后续 source.fakeElement/custom range 再把展开坐标组合到物理 PSI 上。
 */
interface CfirRawSourceElementFactory {
    companion object {
        val TRIVIA_KINDS: Set<String> = CjTokens.COMMENTS.types.mapTo(hashSetOf()) { it.toString() }.apply {
            add(CjTokens.WHITE_SPACE.toString())
        }
    }

    fun createSource(source: CjSourceElement): CjSourceElement
    /** fake source 派生传递确证的原 source 身份，保留 retained child 的 producer。 */
    fun createDerivedSource(original: CjSourceElement, derived: CjSourceElement): CjSourceElement = createSource(derived)
    fun createMacroToken(token: MacroSurfaceToken, source: CjSourceElement): MacroSurfaceToken

    /**
     * 普通源码保留自身 source。每个 builder 独占行表缓存，在 raw 构造的读访问内捕获，
     * executor 只读取冻结坐标，不在执行动态库时访问 PSI，也不产生全局 PSI 强引用缓存。
     */
    class Original : CfirRawSourceElementFactory {
        private val lineMappings = IdentityHashMap<Any, CjSourceFileLinesMapping>()

        override fun createSource(source: CjSourceElement): CjSourceElement = source

        override fun createMacroToken(token: MacroSurfaceToken, source: CjSourceElement): MacroSurfaceToken {
            if (token.text.isEmpty()) return token
            val identity = source.macroSourceTreeIdentity()
            val lines = lineMappings.getOrPut(identity) {
                val text = when (source) {
                    is CjPsiSourceElement -> checkNotNull(source.psi.containingFile).text
                    is CjLightSourceElement -> source.treeStructure.toString(source.treeStructure.root)
                    else -> error("Macro input tokens require a text source tree.")
                }
                text.toSourceLinesMapping()
            }
            val original = source.withRange(token.startOffset, token.endOffset)
            val origin = MacroTokenSourceOrigin.Input(
                original, token.text, isLinear = true, linesMapping = lines, sourceTreeIdentity = identity,
            )
            return token.copy(sourceSpans = listOf(MacroTokenSourceSpan(0, token.text.length, origin)))
        }
    }
}
