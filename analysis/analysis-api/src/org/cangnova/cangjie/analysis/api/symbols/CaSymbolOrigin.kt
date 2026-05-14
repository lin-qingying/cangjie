package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号的来源枚举。
 *
 * 该枚举描述了符号是从哪种数据源或生成机制中产生的（源码、库、编译器合成等），
 * 与 [CaSymbolLocation] 的"位置"职责正交：origin 关注 _怎么来的_，location 关注 _写在哪_。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolOrigin`，但保留仓颉自己的扩展语义
 * （如 [GENERIC_INSTANTIATION]、[EXTENSION]）。
 */
enum class CaSymbolOrigin {
    /**
     * 来源不明。
     *
     * 通常用于尚未完成构建或在错误恢复场景下无法确定来源时的兜底值。
     */
    UNKNOWN,

    /**
     * 直接来自仓颉源码中的声明。
     */
    SOURCE,

    /**
     * 来自已编译的仓颉库（`.cjo` / 已发布产物）。
     */
    LIBRARY,

    /**
     * 由编译器合成的声明。
     *
     * 例如 data class 自动生成的成员、enum 类的 `valueOf` / `values` 等。
     */
    SYNTHETIC,

    /**
     * 由语言规则隐式补齐的默认声明。
     *
     * 例如未显式书写时由编译器补出的默认构造器、默认实现等。
     */
    IMPLICIT_DEFAULT,

    /**
     * 多继承场景下，编译器为 callable 交集合成的声明。
     *
     * #### 示例
     *
     * ```
     * interface A { fun foo() }
     * interface B { fun foo() }
     *
     * interface C : A, B
     * ```
     *
     * 编译器会在 `C` 上以 [INTERSECTION_OVERRIDE] origin 合成 `C.foo`，
     * 表示该函数来自 `A.foo` 与 `B.foo` 的交集。
     */
    INTERSECTION_OVERRIDE,

    /**
     * 由泛型实例化合成的声明。
     *
     * 通用泛型声明被具体类型实参实例化后，针对实例化结果生成的符号采用该 origin。
     */
    GENERIC_INSTANTIATION,

    /**
     * 由 `extend` 声明引入的扩展成员。
     *
     * 这是仓颉特有的 origin：扩展成员既不属于源码中类体内的成员，
     * 也不是编译器纯粹合成的产物，而是显式 `extend` 声明的产出。
     */
    EXTENSION,

    /**
     * SAM（Single Abstract Method）转换合成的构造器。
     *
     * 用于把符合 SAM 形状的函数对象适配成对应接口实例的合成调用入口。
     */
    SAM_CONSTRUCTOR,

    /**
     * 泛型类型替换场景下，在声明侧合成的覆盖声明。
     *
     * 例如父类有泛型成员，子类在固定类型实参后会在 _声明侧_ 合成一个对应的特化覆盖。
     */
    SUBSTITUTION_OVERRIDE_DECLARATION_SITE,

    /**
     * 泛型类型替换场景下，在调用侧合成的覆盖声明。
     *
     * 与 [SUBSTITUTION_OVERRIDE_DECLARATION_SITE] 相对：在 _调用点_ 进行类型替换后
     * 视图化出的覆盖签名。
     */
    SUBSTITUTION_OVERRIDE_CALL_SITE,
}
