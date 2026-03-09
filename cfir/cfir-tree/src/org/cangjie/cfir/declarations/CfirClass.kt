package org.cangjie.cfir.declarations

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.symbols.CfirClassSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 仓颉类种类（对应不同的命名类型声明）。
 */
enum class CfirClassKind {
    /** class 引用类型 */
    CLASS,
    /** interface 接口类型 */
    INTERFACE,
    /** struct 值类型（仓颉特有） */
    STRUCT,
    /** enum 代数数据类型（仓颉特有） */
    ENUM,
}

/**
 * 类声明（class/interface/struct/enum），
 * 对应仓颉编译器中的 ClassDecl / InterfaceDecl / StructDecl / EnumDecl。
 */
class CfirClass(
    override val source: CfirSourceElement? = null,
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
    override val moduleData: CfirModuleData,
    override val annotations: List<CfirAnnotation> = emptyList(),
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    override val status: CfirDeclarationStatus = CfirDeclarationStatus.DEFAULT,
    override val typeParameters: List<CfirTypeParameter> = emptyList(),
    override val superTypeRefs: MutableList<CfirTypeRef> = mutableListOf(),
    override val declarations: MutableList<CfirDeclaration> = mutableListOf(),
    val name: Name,
    val classKind: CfirClassKind,
) : CfirClassLikeDeclaration {
    override val symbol: CfirClassSymbol = CfirClassSymbol()
    override var resolvePhase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirClass {
        superTypeRefs.replaceAll { it.accept(transformer, data) as CfirTypeRef }
        declarations.replaceAll { it.accept(transformer, data) as CfirDeclaration }
        return this
    }
}
