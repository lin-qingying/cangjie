package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin

/**
 * 描述一个 class-like 成员图所处的 owner 视角。
 *
 * 名义 owner 与 extend owner 的直接成员来源不同；沿父类型构造 scope 时保留 extend owner
 * 身份，但直接成员来源恢复为该父类型自身的完整 use-site 集合。
 */
sealed interface CfirMemberScopeOwner {
    /** 当前 owner extend；名义 owner 与普通 use-site scope 没有此项。 */
    val extendOwner: CfirExtend?

    /** 当前 owner 图是否通过 extend provider 查询扩展成员。 */
    val includesExtendScopes: Boolean

    /** 判断给定 extend 是否能作为当前节点的直接成员来源。 */
    fun includesDirectExtend(extend: CfirExtend): Boolean

    /** 判断给定 extend 的 private 成员是否属于当前 owner 视图。 */
    fun includesPrivateMembersFrom(extend: CfirExtend): Boolean

    /** 判断父边是否属于当前 owner 的直接成员图。 */
    fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean

    /** 构造继承节点的 owner 视角。 */
    fun forInheritedScope(): CfirMemberScopeOwner

    /** 普通调用查找：纳入 receiver 上所有适用的 extend。 */
    data object UseSite : CfirMemberScopeOwner {
        override val extendOwner: CfirExtend? = null
        override val includesExtendScopes: Boolean = true
        override fun includesDirectExtend(extend: CfirExtend): Boolean = true
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = true
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean = true
        override fun forInheritedScope(): CfirMemberScopeOwner = InheritedUseSite
    }

    /** 普通继承节点：保留父类型的可见 direct extend，排除不可继承的 private extend 成员。 */
    data object InheritedUseSite : CfirMemberScopeOwner {
        override val extendOwner: CfirExtend? = null
        override val includesExtendScopes: Boolean = true
        override fun includesDirectExtend(extend: CfirExtend): Boolean = true
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = false
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean = true
        override fun forInheritedScope(): CfirMemberScopeOwner = this
    }

    /** 泛型名义 owner：当前节点不纳入直接 extend，父类型仍按普通 use-site 规则展开。 */
    data object NominalInstantiation : CfirMemberScopeOwner {
        override val extendOwner: CfirExtend? = null
        override val includesExtendScopes: Boolean = true
        override fun includesDirectExtend(extend: CfirExtend): Boolean = false
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = true
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean =
            origin !is CfirInstantiatedSupertypeOrigin.Extend || origin.propagationPath.isNotEmpty()

        override fun forInheritedScope(): CfirMemberScopeOwner = InheritedUseSite
    }

    /** CJMP counterpart 不收集当前节点的 direct extend owner 成员。 */
    data object DeclarationOnlyInstantiation : CfirMemberScopeOwner {
        override val extendOwner: CfirExtend? = null
        override val includesExtendScopes: Boolean = false
        override fun includesDirectExtend(extend: CfirExtend): Boolean = false
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = true
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean =
            origin !is CfirInstantiatedSupertypeOrigin.Extend || origin.propagationPath.isNotEmpty()

        override fun forInheritedScope(): CfirMemberScopeOwner = DeclarationOnlyInheritance
    }

    /** 声明级继承图：不收集 extend 成员，但仍保留父类型声明的 extend 接口边。 */
    data object DeclarationOnlyInheritance : CfirMemberScopeOwner {
        override val extendOwner: CfirExtend? = null
        override val includesExtendScopes: Boolean = false
        override fun includesDirectExtend(extend: CfirExtend): Boolean = false
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = true
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean = true
        override fun forInheritedScope(): CfirMemberScopeOwner = this
    }

    /** 泛型 extend owner：保留该 owner 及官方判定为同组的直接 peer。 */
    data class ExtendInstantiation(
        override val extendOwner: CfirExtend,
        val relatedExtends: Set<CfirExtend>,
    ) : CfirMemberScopeOwner {
        override val includesExtendScopes: Boolean = true

        init {
            require(extendOwner in relatedExtends)
        }

        override fun includesDirectExtend(extend: CfirExtend): Boolean = extend in relatedExtends
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = extend === extendOwner

        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean =
            origin !is CfirInstantiatedSupertypeOrigin.Extend ||
                    origin.propagationPath.isNotEmpty() ||
                    origin.sourceExtend in relatedExtends

        override fun forInheritedScope(): CfirMemberScopeOwner = InheritedExtend(extendOwner)
    }

    /** 继承中的 extend owner：沿父类型保留 private-owner 视角，但不过滤父节点自己的直接 extend。 */
    data class InheritedExtend(
        override val extendOwner: CfirExtend,
    ) : CfirMemberScopeOwner {
        override val includesExtendScopes: Boolean = true
        override fun includesDirectExtend(extend: CfirExtend): Boolean = true
        override fun includesPrivateMembersFrom(extend: CfirExtend): Boolean = extend === extendOwner
        override fun includesDirectSupertype(origin: CfirInstantiatedSupertypeOrigin): Boolean = true
        override fun forInheritedScope(): CfirMemberScopeOwner = this
    }
}
