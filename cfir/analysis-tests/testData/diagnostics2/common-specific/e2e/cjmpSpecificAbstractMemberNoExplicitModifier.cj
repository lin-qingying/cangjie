// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测：specific abstract 类中
// 函数消息：'specific' abstract class function must have explicit 'specific' or 'abstract' modifier。
// 属性消息：'specific' abstract class property must have explicit 'specific' or 'abstract' modifier。
// 两者均锚成员名；无访问器属性另报 property must have accessors。函数体/getter及显式 specific/abstract 成员
// 不报该 CJMP 诊断。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common abstract class A {
    public common init()
    public common func specificFunction(): Int64
    public common prop specificProperty: Int64
    public common abstract func abstractFunction(): Int64
    public common abstract prop abstractProperty: Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific abstract class A {
    public specific init() {}
    public func <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>f<!>(): Int64
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public prop <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>p<!>: Int64<!>

    public func concreteFunction(): Int64 { 1 }
    public prop concreteProperty: Int64 {
        get() { 1 }
    }

    public specific func specificFunction(): Int64
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public specific prop specificProperty: Int64<!>
    public specific abstract func abstractFunction(): Int64
    public specific abstract prop abstractProperty: Int64
}
