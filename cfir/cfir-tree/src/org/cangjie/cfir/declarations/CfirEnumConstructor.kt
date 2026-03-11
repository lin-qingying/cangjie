package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 枚举成员声明，对应仓颉编译器中 EnumDecl 内的构造器。
 *
 * 仓颉的 enum 是 ADT，枚举成员可以带有参数。
 * 例如：
 *   enum Color {
 *     Red | Green | Blue
 *     RGB(Int64, Int64, Int64)
 *   }
 */
class CfirEnumConstructor(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    val name: Name,
    /** 枚举构造器参数类型列表（ADT 构造器可带参数） */
    val parameterTypeRefs: List<CfirTypeRef> = emptyList(),
    /** 初始化表达式（简单枚举的关联值） */
    var initializer: CfirExpression? = null,
) : CfirDeclaration {
    override val symbol: CfirEnumConstructorSymbol = CfirEnumConstructorSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumConstructor(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumConstructor {
        initializer = initializer?.let { it.accept(transformer, data) as CfirExpression }
        return this
    }
}
