package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.metadata.model.AttributePack
import java.util.Collections
import java.util.IdentityHashMap

/** 属性投影与省略 Type 的决定属于同一次准备，不能只写 UNREACHABLE 而继续输出原 Type。 */
internal data class CjoFrozenDeclarationAttributes(
    val words: List<ULong>,
    val omitsType: Boolean,
    val partialFunction: Boolean,
)

internal class CjoDeclarationAttributeInputs(
    analysis: CfirDeclarationExportAnalysis,
    sources: CjoFrozenSourceInputs,
    sourceFormats: CjoSourceFormatInputs,
) {
    private val values = IdentityHashMap<CfirDeclaration, CjoFrozenDeclarationAttributes>()

    init {
        analysis.capturedDeclarations().forEach { declaration ->
            val source = sourceFormats.declarations[declaration]
            val loaded = sources.formats[declaration]
            if (source == null && loaded == null) return@forEach
            val originalWords = checkNotNull(loaded?.attributes ?: source?.attributes)
            check(originalWords.size == AttributePack.arraySize) { "A CJO attribute product must contain the complete Native bitset" }
            val original = AttributePack(originalWords)
            val factoryBuiltin = source != null && (source.builtin != null ||
                declaration is CfirTypeParameter && analysis.owningParentOf(declaration) is CfirBuiltInDeclaration)
            val words = AttributePack(originalWords)
            fun set(attribute: Attribute, enabled: Boolean) {
                words.setAttr(attribute, enabled)
            }
            val status = analysis.statusOf(declaration)
            val preceding = analysis.precedingFactsOf(declaration)
            val partial = declaration is CfirFunction && preceding.isGenericInstantiated && analysis.hasOwnGenericNode(declaration)
            if (!factoryBuiltin) {
                set(Attribute.IMPORTED, preceding.isImported)
                set(Attribute.COMMON, status.isCommon)
                set(Attribute.SPECIFIC, status.isSpecific)
                if (sourceFormats.writesCjmpCommonPart) set(Attribute.FROM_COMMON_PART, true)
                if (preceding.isGenericInstantiated && analysis.hasOwnGenericNode(declaration)) set(Attribute.UNREACHABLE, true)
                if ((loaded?.kind ?: source?.kind) == PackageFormat.DeclKind.VarDecl) {
                    val outerStatus = analysis.nativeOuterOf(declaration)?.let(analysis::statusOf)
                    if (original.testAttr(Attribute.FROM_COMMON_PART) && outerStatus?.isSpecific == true) {
                        set(Attribute.COMMON, false)
                        set(Attribute.FROM_COMMON_PART, false)
                    }
                    if (outerStatus?.isCommon == true) {
                        set(Attribute.INITIALIZED, loaded?.hasInitializer ?: checkNotNull(source).hasInitializer)
                        if (status.isStatic && original.testAttr(Attribute.INITIALIZED)) set(Attribute.INITIALIZED, true)
                    }
                    set(Attribute.SPECIFIC, false)
                }
            }
            values[declaration] = CjoFrozenDeclarationAttributes(Collections.unmodifiableList(words.toULongList()),
                words.testAttr(Attribute.UNREACHABLE), partial)
        }
    }

    fun attributesOf(declaration: CfirDeclaration): CjoFrozenDeclarationAttributes = values.getValue(declaration)
}

