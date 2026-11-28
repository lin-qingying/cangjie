// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测：common abstract 类中无体且未写 common/abstract 的
// 函数消息：'common' abstract class function must have explicit 'common' or 'abstract' modifier。
// 属性消息：'common' abstract class property must have explicit 'common' or 'abstract' modifier。
// 两者均锚成员名；无访问器属性另报 property must have accessors。带函数体/getter、显式 common 或 abstract 的
// 成员均不报该 CJMP 诊断；显式 static abstract 的无体成员只报 MISSING_FUNC_BODY。
package cjmp_p

public common abstract class A {
    public common init()
    public func <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>f<!>(): Int64
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public prop <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>p<!>: Int64<!>

    public func concreteFunction(): Int64 { 1 }
    public prop concreteProperty: Int64 {
        get() { 1 }
    }

    public common func commonFunction(): Int64
    public common prop commonProperty: Int64
    public abstract func abstractFunction(): Int64
    public abstract prop abstractProperty: Int64
    public static abstract <!MISSING_FUNC_BODY!>func staticAbstractFunction(): Int64<!>
    public static abstract <!MISSING_FUNC_BODY!>prop staticAbstractProperty: Int64<!>
}
