package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.utils.WeakPair

/**
 * 基于 [ClassId] 的 class-like lookup tag 实现。
 *
 * lookup tag 的结构身份只由 [classId] 决定；[boundSymbol] 只是按 session 缓存的弱绑定结果，
 * 不参与 equals/hashCode。
 *
 * @property classId 当前 tag 对应的 class-like 声明 id。
 */
class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {


    /**
     * 当前 tag 在某个 session 中解析到的弱引用符号缓存。
     */
    var boundSymbol: WeakPair<CfirSession, CfirClassLikeSymbol<*>?>? = null

    /**
     * 基于 [classId] 比较 lookup tag 结构身份。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagImpl

        if (classId != other.classId) return false

        return true
    }

    /**
     * 返回 [classId] 的哈希值。
     */
    override fun hashCode(): Int {
        return classId.hashCode()
    }
}
