package org.cangjie.cfir.session

import org.cangnova.cangjie.util.ArrayMapAccessor
import org.cangnova.cangjie.util.ComponentArrayOwner
import org.cangnova.cangjie.util.NullableArrayMapAccessor
import org.cangnova.cangjie.util.TypeRegistry
import kotlin.reflect.KClass

/**
 * 编译器 session：组件注册中心（对齐 Kotlin 的 FirSession）。
 *
 * 继承 ComponentArrayOwner，使用数组 + TypeRegistry 实现 O(1) 组件查找。
 * session 按 Kind 区分：Source（编译本模块）、Library（读取 .cjo 依赖）。
 */
abstract class CfirSession(
    val kind: Kind,
) : ComponentArrayOwner<CfirSessionComponent, CfirSessionComponent>() {

    companion object : TypeRegistry<CfirSessionComponent, CfirSessionComponent>() {
        inline fun <reified T : CfirSessionComponent> sessionComponentAccessor(): ArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateAccessor(T::class)
        }

        inline fun <reified T : CfirSessionComponent> nullableSessionComponentAccessor(): NullableArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateNullableAccessor(T::class)
        }
    }

    final override val typeRegistry: TypeRegistry<CfirSessionComponent, CfirSessionComponent> = Companion

    fun register(tClass: KClass<out CfirSessionComponent>, value: CfirSessionComponent) {
        registerComponent(tClass, value)
    }

    enum class Kind {
        /** 源码编译模式 */
        Source,
        /** 库依赖模式（读取已编译的 .cjo） */
        Library,
    }
}
