package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.patterns.CfirBindingPattern
import org.cangjie.cfir.patterns.CfirEnumPattern
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.patterns.CfirTuplePattern
import org.cangjie.cfir.patterns.CfirTypePattern
import org.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 属性声明（含可选的 getter/setter），对应仓颉编译器中的 PropDecl。
 */
class CfirProperty(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val name: Name,
    /** 初始值表达式 */
    var initializer: CfirExpression? = null,
    val getter: CfirFunction? = null,
    val setter: CfirFunction? = null,
    /** 是否为 var（可变）声明 */
    val isVar: Boolean = false,
) : CfirCallableDeclaration {
    override val symbol: CfirPropertySymbol = CfirPropertySymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirProperty {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        initializer = initializer?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 变量声明（局部变量），对应仓颉编译器中的 VarDecl。
 */
class CfirVariable(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val name: Name,
    var initializer: CfirExpression? = null,
    /** 是否为 var（可变）声明 */
    val isVar: Boolean = false,
) : CfirCallableDeclaration {
    override val symbol: CfirVariableSymbol = CfirVariableSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVariable {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        initializer = initializer?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}

/**
 * 模式变量声明，对应带完整 pattern 的变量声明入口。
 *
 * 与 [CfirVariable] 不同，模式变量可能导出 0..N 个绑定，
 * 因此不以单一 name 建模。
 */
class CfirPatternVariable(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override var returnTypeRef: CfirTypeRef,
    val pattern: CfirPattern,
    var initializer: CfirExpression? = null,
    val isVar: Boolean = false,
) : CfirCallableDeclaration {
    override val symbol: CfirPatternVariableSymbol = CfirPatternVariableSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    val bindings: List<CfirBindingPattern>
        get() = collectBindings(pattern)

    val allPatternDeclarations: List<CfirPattern>
        get() = collectPatternDeclarations(pattern)

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPatternVariable(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPatternVariable {
        returnTypeRef = returnTypeRef.accept(transformer, data) as CfirTypeRef
        initializer = initializer?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }

    private fun collectBindings(pattern: CfirPattern): List<CfirBindingPattern> = when (pattern) {
        is CfirBindingPattern -> listOf(pattern) + (pattern.nestedPattern?.let { collectBindings(it) } ?: emptyList())
        is CfirTuplePattern -> pattern.elements.flatMap { collectBindings(it) }
        is CfirEnumPattern -> pattern.arguments.flatMap { collectBindings(it) }
        else -> emptyList()
    }

    private fun collectPatternDeclarations(pattern: CfirPattern): List<CfirPattern> {
        val current = when (pattern) {
            is CfirBindingPattern -> listOf(pattern)
            is CfirTypePattern -> if (pattern.bindingName != null) listOf(pattern) else emptyList()
            else -> emptyList()
        }
        val nested = when (pattern) {
            is CfirBindingPattern -> pattern.nestedPattern?.let { collectPatternDeclarations(it) } ?: emptyList()
            is CfirTuplePattern -> pattern.elements.flatMap { collectPatternDeclarations(it) }
            is CfirEnumPattern -> pattern.arguments.flatMap { collectPatternDeclarations(it) }
            else -> emptyList()
        }
        return current + nested
    }
}
