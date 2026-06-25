package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 内建 primitive 类型在 CFIR 声明树中的类声明实现。
 *
 * primitive 类型不是用户源码中的普通 class，但需要以 [CfirClassLikeDeclaration] 的形态进入
 * scope、类型构造器、继承检查和渲染流程。因此该节点手写实现生成式 CFIR 类声明所要求的
 * visitor / transformer / replace 契约，同时固定 [source] 为 `null`。
 *
 * @property moduleData 声明所属模块。
 * @property symbol 绑定该 primitive 声明的符号。
 * @property name primitive 类型名称。
 * @property kind primitive 类型分类，用于连接内建类型表与语义规则。
 * @property scopeProvider primitive 类型成员 scope 的提供者。
 * @property annotations 声明上的注解列表。
 * @property origin 声明来源，默认视为编译器 synthetic。
 * @property attributes 声明扩展属性容器。
 * @property typeParameters primitive 类型声明携带的类型参数列表。
 * @property status 声明状态与修饰符信息。
 * @property deprecationsProvider 弃用信息提供者。
 * @property declarations primitive 类型内部声明。
 * @property superTypeRefs primitive 类型父类型引用。
 */
@OptIn(ResolveStateAccess::class, CfirImplementationDetail::class)
class CfirPrimitiveTypeDeclaration(
    /**
     * 声明所属模块。
     */
    override val moduleData: CfirModuleData,
    /**
     * 绑定该 primitive 声明的符号。
     */
    override val symbol: CfirPrimitiveTypeSymbol,
    /**
     * primitive 类型名称。
     */
    override val name: Name,
    /**
     * primitive 类型分类，用于连接内建类型表与语义规则。
     */
    val kind: PrimitiveTypeKind,
    /**
     * primitive 类型成员 scope 的提供者。
     */
    override val scopeProvider: CfirScopeProvider,
    /**
     * 声明上的注解列表。
     */
    override var annotations: MutableOrEmptyList<CfirAnnotation> = MutableOrEmptyList.empty(),
    /**
     * 声明来源，默认视为编译器 synthetic。
     */
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Synthetic.Default,
    /**
     * 声明扩展属性容器。
     */
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    /**
     * primitive 类型声明携带的类型参数列表。
     */
    override var typeParameters: MutableList<CfirTypeParameterRef> = mutableListOf(),
    /**
     * 声明状态与修饰符信息。
     */
    override var status: CfirDeclarationStatus = org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl(),
    /**
     * 弃用信息提供者。
     */
    override var deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider,
    /**
     * primitive 类型内部声明。
     */
    override var declarations: MutableList<CfirDeclaration> = mutableListOf(),
    /**
     * primitive 类型父类型引用。
     */
    override var superTypeRefs: MutableList<CfirTypeRef> = mutableListOf(),
) : CfirClassLikeDeclaration() {
    /**
     * primitive 类型由编译器内建表生成，没有对应源码节点。
     */
    override val source: CjSourceElement? = null


    init {
        symbol.bind(this)
        resolveState = CfirResolvePhase.RAW_CFIR.asResolveState()
    }

    /**
     * 按生成式 CFIR 树的子节点顺序访问 primitive 类型声明的子结构。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    /**
     * 替换声明注解列表。
     */
    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()

    }

    /**
     * 替换声明状态对象。
     */
    override fun replaceStatus(newStatus: CfirDeclarationStatus) {
        status = newStatus
    }

    /**
     * 替换弃用信息提供者。
     */
    override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider) {
        deprecationsProvider = newDeprecationsProvider
    }

    /**
     * 原地转换注解子节点并返回当前声明。
     */
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        annotations.transformInplace(transformer, data)

        return this
    }

    /**
     * 转换类型参数子节点并保持可变列表承载。
     */
    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        typeParameters = typeParameters.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeParameterRef }.toMutableList()
        return this
    }

    /**
     * 转换声明状态节点。
     */
    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        status = status.transform<CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    /**
     * 转换 primitive 类型内部声明列表。
     */
    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        declarations = declarations.map { it.transform<CfirElement, D>(transformer, data) as CfirDeclaration }.toMutableList()
        return this
    }

    /**
     * 转换父类型引用列表。
     */
    override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        superTypeRefs = superTypeRefs.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeRef }.toMutableList()
        return this
    }

    /**
     * 按生成式 CFIR 节点顺序转换所有子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPrimitiveTypeDeclaration {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformDeclarations(transformer, data)
        transformSuperTypeRefs(transformer, data)
        return this
    }
}
