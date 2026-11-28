// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T08_DifferentSuperType
// 超类型集合不同：`'specific' class super types is not match 'common' super types`（锚 specific 声明，官方 MatchCommonNominalDeclWithSpecific）
// 补充证据：cjc 1.1.3 两段式探针 .workbuddy/tmp/cjmp_probe113/e3_supertype_identity。
// 等量接口须比较类型身份及类型实参；common 泛型参数映射到 specific 后比较，接口顺序不影响兼容性。
// 未解析接口证据：e3_error_supertype；过滤错误类型后比较有效接口集合，错误引用顺序不改变结果。
// 非接口、重复接口及 interface 非法继承 class：e3_error_supertype / e3_error_interface_class，均只报继承自身的错误。
// 自继承及错误泛型实参数量：e3_error_interface_loop / e3_error_generic_arity；前者无 CJMP 差异，后者仍报告缺少有效接口。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public interface I {}
public interface Other {}
public interface Generic<T> {}
public open class BaseA { public init() {} }
public open class BaseB { public init() {} }

public common class S <: I {
    public common init()
}

public common class DifferentInterface <: I {
    public common init()
}

public common class DifferentArgument <: Generic<Int64> {
    public common init()
}

public common class RenamedParameter<T> <: Generic<T> {
    public common init()
}

public common class ReorderedInterfaces <: I & Other {
    public common init()
}

public common class DifferentSuperclass <: BaseA {
    public common init()
}

public common class OnlyUnresolvedInterface <: I {
    public common init()
}

public common class TrailingUnresolvedInterface <: I {
    public common init()
}

public common class LeadingUnresolvedInterface <: I {
    public common init()
}

public common class NonInheritableExtra <: I {
    public common init()
}

public common class DuplicateInterface <: I {
    public common init()
}

public common interface IllegalInterfaceSuperclass <: I {}

public common interface SelfInterfaceLoop {}

public common class InvalidGenericArity <: Generic<Int64> {
    public common init()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class S {
    public specific init() {}
}<!>

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class DifferentInterface <: Other {
    public specific init() {}
}<!>

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class DifferentArgument <: Generic<Bool> {
    public specific init() {}
}<!>

public specific class RenamedParameter<U> <: Generic<U> {
    public specific init() {}
}

public specific class ReorderedInterfaces <: Other & I {
    public specific init() {}
}

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class DifferentSuperclass <: BaseB {
    public specific init() {}
}<!>

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class OnlyUnresolvedInterface <: <!UNDECLARED_TYPE_NAME!>Missing<!> {
    public specific init() {}
}<!>

public specific class TrailingUnresolvedInterface <: I & <!UNDECLARED_TYPE_NAME!>Missing<!> {
    public specific init() {}
}

public specific class LeadingUnresolvedInterface <: <!UNDECLARED_TYPE_NAME!>Missing<!> & I {
    public specific init() {}
}

public specific class NonInheritableExtra <: I & <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>Int64<!> {
    public specific init() {}
}

public specific class <!SUPER_TYPES_DUPLICATE!>DuplicateInterface<!> <: I & I {
    public specific init() {}
}

public specific interface <!INTERFACE_CANNOT_INHERIT_CLASS!>IllegalInterfaceSuperclass<!> <: I & BaseA {}

public specific interface SelfInterfaceLoop <: <!SUPER_TYPES_SELF_REFERENCE!>SelfInterfaceLoop<!> {}

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class InvalidGenericArity <: <!GENERIC_ARGUMENT_NO_MATCH!>Generic<Int64, Bool><!> {
    public specific init() {}
}<!>
