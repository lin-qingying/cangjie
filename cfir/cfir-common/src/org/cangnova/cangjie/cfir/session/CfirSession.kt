package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.ConeTypeRegistry
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
 *
 * @property kind 当前 session 的输入来源与解析模式。
 */
abstract class CfirSession(
    /** 当前 session 的输入来源与解析模式。 */
    val kind: Kind,
) : ComponentArrayOwner<CfirSessionComponent, CfirSessionComponent>() {

    /**
     * session component 的访问器工厂和全局注册表。
     */
    companion object : ConeTypeRegistry<CfirSessionComponent, CfirSessionComponent>() {
        /**
         * 为类型 [T] 创建必需组件访问器。
         */
        inline fun <reified T : CfirSessionComponent> sessionComponentAccessor(): ArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateAccessor(T::class)
        }

        /**
         * 为类型 [T] 创建带默认实现的必需组件访问器。
         */
        @Suppress("INVISIBLE_REFERENCE")
        inline fun <reified T : CfirSessionComponent> sessionComponentAccessorWithDefault(
            defaultImplementation: @kotlin.internal.NoInfer T,
        ): ArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateAccessor(T::class, defaultImplementation)
        }

        /**
         * 为自定义字符串 [id] 创建必需组件访问器。
         */
        inline fun <reified T : CfirSessionComponent> sessionComponentAccessor(id: String): ArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateAccessor(id)
        }

        /**
         * 为类型 [T] 创建可为空组件访问器。
         */
        inline fun <reified T : CfirSessionComponent> nullableSessionComponentAccessor(): NullableArrayMapAccessor<CfirSessionComponent, CfirSessionComponent, T> {
            return generateNullableAccessor(T::class)
        }
    }

    /**
     * session component 数组使用的注册表。
     */
    final override val typeRegistry: TypeRegistry<CfirSessionComponent, CfirSessionComponent> = Companion

    /**
     * 按组件类型注册 [value]。
     */
    fun register(tClass: KClass<out CfirSessionComponent>, value: CfirSessionComponent) {
        registerComponent(tClass, value)
    }

    /**
     * 按自定义限定名注册 [value]。
     */
    fun register(keyQualifiedName: String, value: CfirSessionComponent) {
        registerComponent(keyQualifiedName, value)
    }

    /**
     * session 的数据来源类别。
     */
    enum class Kind {
        /** 源码编译模式 */
        Source,
        /** 库依赖模式（读取已编译的 .cjo） */
        Library,
    }
}

/**
 * visitor 或 processor 在遍历过程中的控制动作。
 */
enum class ProcessorAction {
    /**
     * 立即停止后续处理。
     */
    STOP,

    /**
     * 明确继续进入下一个处理步骤。
     */
    NEXT,

    /**
     * 当前处理器不改变已有动作。
     */
    NONE;

    /**
     * 将 [STOP] 转换为 `true`，供旧式布尔控制流使用。
     */
    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
            NONE -> false
        }
    }

    /**
     * 当前动作是否要求停止处理。
     */
    fun stop(): Boolean = this == STOP

    /**
     * 当前动作是否允许继续处理。
     */
    fun next(): Boolean = this != STOP

    /**
     * 合并两个处理动作。
     *
     * [NEXT] 优先级最高，表示任一处理器要求继续时整体继续；否则保留当前动作。
     */
    operator fun plus(other: ProcessorAction): ProcessorAction {
        if (this == NEXT || other == NEXT) return NEXT
        return this
    }
}
