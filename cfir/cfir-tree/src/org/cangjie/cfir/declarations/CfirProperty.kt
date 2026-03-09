package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirPropertySymbol
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
