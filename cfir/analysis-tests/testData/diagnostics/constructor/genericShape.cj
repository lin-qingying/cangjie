// RUN_PIPELINE_TILL: FRONTEND
//
// P3 批次 1（泛型/函数形态/实例化族）四项诊断的回归语料。
// 官方语义均以 cjc 1.0.5 探针取证（/tmp/cjcprobe_gen）：
//   - sema_forbid_generic_constructor:  构造器自身类型参数非法，类级类型参数不算
//   - sema_generic_in_operator_overload: operator 重载不能声明类型参数
//   - sema_abstract_class_can_not_be_instantiated: this()/super() 豁免
//   - sema_numeric_convert_must_be_numeric: 数值/Rune/Nothing 白名单外报错
class GenericCtor {
    <!FORBID_GENERIC_CONSTRUCTOR!>init<!><T>(x: T) {}
}

class GenericClassCtor<T> {
    init(x: T) {}
}

class OpHolder {
    public operator func <!GENERIC_IN_OPERATOR_OVERLOAD!>+<!><T>(other: OpHolder): OpHolder {
        this
    }
}

public abstract class AbsBase {
    public init() {}
}

class Derived <: AbsBase {}

func instantiate(): Unit {
    let _ = <!ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED!>AbsBase<!>()
    let _ = Derived()
}

func numConvert(b: Bool, i: Int64): Unit {
    // CFIR 的数值转换语义由 CfirTypeConversion 结果类型承载（Cone 诊断映射），
    // 诊断锚定整个转换表达式；官方 cjc 锚在被转换实参，语义相同。
    let _ = <!NUMERIC_CONVERT_MUST_BE_NUMERIC!>Int64(b)<!>
    let _ = UInt32(i)
}
