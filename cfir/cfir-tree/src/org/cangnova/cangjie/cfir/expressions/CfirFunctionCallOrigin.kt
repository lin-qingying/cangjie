package org.cangnova.cangjie.cfir.expressions

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

    val isConstructorDelegation: Boolean
        get() = this == ConstructorDelegationThis || this == ConstructorDelegationSuper
}
