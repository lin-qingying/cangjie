package org.cangnova.cangjie.frontend.pipeline

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.builder.CfirRawSourceElementFactory
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCfirSourceOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMap
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMapping
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceSpan
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.isInSameSourceTree
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.source.withRange

/**
 * 官方 new2originPosMap 在 AST/raw source 创建边界的投影。
 * 查找只沿确证输入坐标的祖先路径；节点种类和完整范围必须同时一致。
 */
internal class MacroFragmentSourceElementFactory(
    private val sourceMap: MacroFragmentSourceMap,
    private val fragmentSource: CjSourceElement,
    private val fragmentStartOffset: Int,
    private val surfaceId: Long,
    private val sourceMapping: MacroFragmentSourceMapping,
    private val inheritedAnnotations: List<CfirAnnotationSlotSnapshot> = emptyList(),
) : CfirRawSourceElementFactory {
    init {
        sourceMapping.registerDiagnosticProjector(fragmentSource, ::projectDiagnosticSource)
    }

    /** 子 token 的诊断投影不修改构造期来源表，只消费已保存的原 token 区间。 */
    private fun projectDiagnosticSource(source: CjSourceElement): CjSourceElement {
        val start = source.startOffset - fragmentStartOffset
        val end = source.endOffset - fragmentStartOffset
        if (start !in 0..sourceMap.text.length || end !in start..sourceMap.text.length) return source
        val original = sourceMap.originalRange(start, end) ?: return source
        return projectSource(source, original, original) ?: source
    }
    override fun annotationSourceModuleName(source: CjSourceElement?, parserModuleName: String): String {
        if (source == null) return parserModuleName
        val original = if (source.isInSameSourceTree(fragmentSource)) {
            sourceMap.originalRange(source.startOffset - fragmentStartOffset, source.endOffset - fragmentStartOffset)
        } else source
        if (original == null) return parserModuleName
        val snapshot = inheritedAnnotations.singleOrNull {
            it.annotationSource.isInSameSourceTree(original) &&
                it.annotationSource.startOffset == original.startOffset && it.annotationSource.endOffset == original.endOffset
        }
        return snapshot?.originalAnnotation?.sourceModuleName ?: parserModuleName
    }
    override fun createSource(source: CjSourceElement): CjSourceElement {
        if (!source.isInSameSourceTree(fragmentSource)) {
            val origin = checkNotNull(sourceMapping.originForSource(source)) {
                "A raw source escaped its macro fragment and registered input/generated source trees."
            }
            sourceMapping.register(source, origin)
            return createdSource(source, source)
        }

        val anchorStart: Int
        val anchorEnd: Int
        when (source) {
            is CjPsiSourceElement -> {
                anchorStart = source.psi.textRange.startOffset
                anchorEnd = source.psi.textRange.endOffset
            }
            is CjLightSourceElement -> {
                anchorStart = source.treeStructure.getStartOffset(source.lighterASTNode)
                anchorEnd = source.treeStructure.getEndOffset(source.lighterASTNode)
            }
            else -> error("Macro reparsing requires a PSI or LightTree source.")
        }

        val originalAnchor = sourceMap.originalRange(
            anchorStart - fragmentStartOffset,
            anchorEnd - fragmentStartOffset,
        )
        val originalDiagnostic = sourceMap.originalRange(
            source.startOffset - fragmentStartOffset,
            source.endOffset - fragmentStartOffset,
        )
        val projected = if (originalAnchor != null && originalDiagnostic != null &&
            originalAnchor.isInSameSourceTree(originalDiagnostic)
        ) {
            projectSource(source, originalAnchor, originalDiagnostic)
        } else {
            null
        }
        if (projected != null) {
            sourceMapping.register(projected, MacroCfirSourceOrigin.PreservedInput)
            return createdSource(source, projected)
        }

        val start = anchorStart - fragmentStartOffset
        val end = anchorEnd - fragmentStartOffset
        val origin = sourceMap.originForRange(start, end)
        if (origin != null) {
            // 纯 Input 的 unprojectable source 仍是 Input，保留展开 PSI 与原区间事实。
            sourceMapping.register(source, origin)
        } else if (start < 0 || end > sourceMap.text.length) {
            // 只有 parser 明确加入的 wrapper 区间由当前构造生成。
            sourceMapping.register(source, MacroCfirSourceOrigin.Generated(setOf(surfaceId)))
        }
        return createdSource(source, source)
    }

    /** 保存实际 reparse 节点到返回 source 的创建关系，供 LL 规范映射消费。 */
    private fun createdSource(reparsed: CjSourceElement, created: CjSourceElement): CjSourceElement {
        sourceMapping.registerCreatedSource(reparsed, created)
        return created
    }

    override fun createDerivedSource(original: CjSourceElement, derived: CjSourceElement): CjSourceElement {
        sourceMapping.registerDerived(original, derived)
        val projected = createSource(derived)
        sourceMapping.registerDerived(original, projected)
        return projected
    }

    override fun createMacroToken(token: MacroSurfaceToken, source: CjSourceElement): MacroSurfaceToken {
        check(source.isInSameSourceTree(fragmentSource)) {
            "Macro token capture must use the reparsed tree, before source projection."
        }
        val start = token.startOffset - fragmentStartOffset
        val end = token.endOffset - fragmentStartOffset
        val spans = if (start in 0..sourceMap.text.length && end in start..sourceMap.text.length) {
            check(sourceMap.text.substring(start, end) == token.text) {
                "A reparsed token must retain its exact rendered fragment range."
            }
            sourceMap.spansForRange(start, end)
        } else if (token.text.isEmpty()) {
            emptyList()
        } else {
            // parser 添加的 wrapper 不属于宏返回的原输入，明确由当前构造拥有。
            listOf(MacroTokenSourceSpan(0, token.text.length, MacroTokenSourceOrigin.Generated(surfaceId)))
        }
        return token.copy(sourceSpans = spans)
    }

    private fun projectSource(
        source: CjSourceElement,
        originalAnchor: CjSourceElement,
        originalDiagnostic: CjSourceElement,
    ): CjSourceElement? {
        val inputPsi = originalAnchor.psi
        if (inputPsi != null) {
            val file = inputPsi.containingFile ?: return null
            var candidate: PsiElement? = file.findElementAt(originalAnchor.startOffset)
            while (candidate != null) {
                if (candidate.node?.elementType == source.elementType &&
                    candidate.textRange.startOffset == originalAnchor.startOffset &&
                    candidate.textRange.endOffset == originalAnchor.endOffset
                ) {
                    val anchored = candidate.toCjPsiSourceElement(source.kind)
                    return if (anchored.startOffset == originalDiagnostic.startOffset &&
                        anchored.endOffset == originalDiagnostic.endOffset
                    ) anchored else anchored.withRange(originalDiagnostic.startOffset, originalDiagnostic.endOffset)
                }
                candidate = candidate.parent
            }
            return null
        }

        val light = originalAnchor as? CjLightSourceElement ?: return null
        val tree = light.treeStructure
        var result: LighterASTNode? = null
        var matches = 0
        fun visit(node: LighterASTNode) {
            val start = tree.getStartOffset(node)
            val end = tree.getEndOffset(node)
            if (start > originalAnchor.startOffset || end < originalAnchor.endOffset) return
            if (node.tokenType == source.elementType && start == originalAnchor.startOffset &&
                end == originalAnchor.endOffset
            ) {
                result = node
                matches++
                return
            }
            val children = Ref<Array<LighterASTNode?>>()
            val count = tree.getChildren(node, children)
            val elements = children.get()
            try {
                for (index in 0 until count) {
                    val child = elements[index] ?: continue
                    visit(child)
                }
            } finally {
                tree.disposeChildren(elements, count)
            }
        }
        visit(tree.root)
        if (matches != 1) return null
        return CjLightSourceElement(
            lighterASTNode = checkNotNull(result),
            startOffset = originalDiagnostic.startOffset,
            endOffset = originalDiagnostic.endOffset,
            treeStructure = tree,
            kind = source.kind,
        )
    }
}
