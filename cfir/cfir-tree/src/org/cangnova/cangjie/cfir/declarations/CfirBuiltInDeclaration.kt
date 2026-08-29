package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 内建类型在 CFIR 声明树中的类声明实现，对应官方 `AST::BuiltInDecl`。
 *
 * 官方在编译 `std.core` 时通过 `AddBuiltIn*Decl` 把 `RawArray` / `VArray` / `CPointer` /
 * `CString` / `CFunc` 作为普通全局声明注入 `pkg.files[0]->decls`，随后走正常符号表构建
 * （`collector.BuildSymbolTable`），并随 `.cjo` 序列化传播。因此这些名字在解析层面就是
 * `std.core` 下的 classifier，不需要在类型解析器中按名字硬编码拦截。
 *
 * 该节点沿用 [CfirPrimitiveTypeDeclaration] 的做法手写生成式 CFIR 类声明所要求的
 * visitor / transformer / replace 契约，并固定 [source] 为 `null`。
 *
 * 两点语义约束：
 *
 * - 官方 `BuiltInDecl` 直接继承 `Decl` 而**非** `InheritableDecl`，因此 [superTypeRefs] 恒为空，
 *   不参与继承检查；`Ty::GetDeclPtrOfTy<InheritableDecl>()` 对这些类型返回 `nullptr`，
 *   这正是官方为内建类型另设 `TypeManager::builtinTyToExtendMap` 的原因。
 * - [typeParameters] 是真实的类型参数（个数见 [CfirBuiltInTypeKind.typeParameterCount]），
 *   `CPointer<T>` 还带 `T <: CType` 约束，需要在构造时挂载。
 *
 * @property moduleData 声明所属模块。
 * @property symbol 绑定该内建声明的符号。
 * @property name 内建类型名称。
 * @property kind 内建类型分类，用于连接声明层内建表与语义规则。
 * @property scopeProvider 内建类型成员 scope 的提供者。
 * @property annotations 声明上的注解列表。
 * @property origin 声明来源。
 * @property attributes 声明扩展属性容器。
 * @property typeParameters 内建类型声明携带的类型参数列表。
 * @property status 声明状态与修饰符信息。
 * @property deprecationsProvider 弃用信息提供者。
 * @property declarations 内建类型内部声明。
 * @property superTypeRefs 内建类型父类型引用，按官方语义恒为空。
 */
@OptIn(ResolveStateAccess::class, CfirImplementationDetail::class)
class CfirBuiltInDeclaration(
    /**
     * 声明所属模块。
     */
    override val moduleData: CfirModuleData,
    /**
     * 绑定该内建声明的符号。
     */
    override val symbol: CfirBuiltInTypeSymbol,
    /**
     * 内建类型名称。
     */
    override val name: Name,
    /**
     * 内建类型分类，用于连接声明层内建表与语义规则。
     */
    val kind: CfirBuiltInTypeKind,
    /**
     * 内建类型成员 scope 的提供者。
     */
    override val scopeProvider: CfirScopeProvider,
    /**
     * 声明上的注解列表。
     */
    override var annotations: MutableOrEmptyList<CfirAnnotation> = MutableOrEmptyList.empty(),
    /**
     * 声明来源。
     */
    override val origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Synthetic.Default,
    /**
     * 声明扩展属性容器。
     */
    override val attributes: CfirDeclarationAttributes = CfirDeclarationAttributes.EMPTY,
    /**
     * 内建类型声明携带的类型参数列表。
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
     * 内建类型内部声明。
     */
    override var declarations: MutableList<CfirDeclaration> = mutableListOf(),
    /**
     * 内建类型父类型引用，按官方语义恒为空。
     */
    override var superTypeRefs: MutableList<CfirTypeRef> = mutableListOf(),
) : CfirClassLikeDeclaration() {
    /**
     * 内建类型由编译器注入，没有对应源码节点。
     */
    override val source: CjSourceElement? = null

    init {
        symbol.bind(this)
        resolveState = CfirResolvePhase.RAW_CFIR.asResolveState()
    }

    /**
     * 按生成式 CFIR 树的子节点顺序访问内建类型声明的子结构。
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
    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        annotations.transformInplace(transformer, data)
        return this
    }

    /**
     * 转换类型参数子节点并保持可变列表承载。
     */
    override fun <D> transformTypeParameters(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        typeParameters = typeParameters.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeParameterRef }.toMutableList()
        return this
    }

    /**
     * 转换声明状态节点。
     */
    override fun <D> transformStatus(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        status = status.transform<CfirElement, D>(transformer, data) as CfirDeclarationStatus
        return this
    }

    /**
     * 转换内建类型内部声明列表。
     */
    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        declarations = declarations.map { it.transform<CfirElement, D>(transformer, data) as CfirDeclaration }.toMutableList()
        return this
    }

    /**
     * 转换父类型引用列表。
     */
    override fun <D> transformSuperTypeRefs(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        superTypeRefs = superTypeRefs.map { it.transform<CfirElement, D>(transformer, data) as CfirTypeRef }.toMutableList()
        return this
    }

    /**
     * 按生成式 CFIR 节点顺序转换所有子节点。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBuiltInDeclaration {
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformDeclarations(transformer, data)
        transformSuperTypeRefs(transformer, data)
        return this
    }
}
