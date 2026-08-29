package org.cangnova.cangjie.cfir.expressions

/**
 * 函数调用节点的来源分类。
 *
 * origin 用于区分普通调用、运算符重写调用、编译器内建调用以及构造器 delegation 调用，
 * 使 resolver 和 checker 可以在同一 [CfirFunctionCall] 结构上执行不同语义。
 */
enum class CfirFunctionCallOrigin {
    /**
     * 普通函数/构造器调用。
     */
    Regular,

    /**
     * 运算符重写调用。
     */
    Operator,

    /**
     * `createMock(...)` / `createSpy(...)` 的 mock intrinsic 调用。
     *
     * 官方编译器会在类型检查后把它识别成 `CALL_INTRINSIC_FUNCTION`，
     * 然后再交给 test/mock 语义阶段处理。
     * 本地 CFIR 由于用户写法并不直接匹配内部 runtime stub 签名，
     * 需要在 raw CFIR 阶段先保留这层特殊入口，避免它退化成普通 unresolved call。
     */
    MockIntrinsic,

    /**
     * `a |> f` 解糖产生的调用。
     *
     * 该来源仍按普通用户调用执行候选解析和可见性检查，但必须保留 flow
     * 语义，供调用完成后的目标类型诊断使用。
     */
    Pipeline,

    /**
     * 编译器生成的 core 包内建调用。
     *
     * 对齐官方 AST `Attribute::IN_CORE`：这类引用不是用户源码中的普通名字查找，
     * 因此不能按用户可见性报告 `INVISIBLE_REFERENCE`。
     */
    CompilerCoreIntrinsic,

    /**
     * `this(...)` 构造器 delegation 调用。
     *
     * 它在语法上仍表现为 call expression，但语义上不能再走普通名字解析，
     * 否则会先退化成 `UNRESOLVED_REFERENCE`，再由 constructor checker 二次识别。
     */
    ConstructorDelegationThis,

    /**
     * `super(...)` 构造器 delegation 调用。
     */
    ConstructorDelegationSuper,
    ;

    /**
     * 当前 origin 是否表示构造器 delegation 调用。
     */
    val isConstructorDelegation: Boolean
        get() = this == ConstructorDelegationThis || this == ConstructorDelegationSuper
}
