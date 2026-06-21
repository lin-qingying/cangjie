// FILE: hide.cj


package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {
    }
}

// FILE: hide_use1.cj


package ohos.dep

import ohos.labels.*

@!Hide[isChecked: true]
public open class A {
    @!Hide[isChecked: true]
    public var a = 1
    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public A(public var v!: Int64 = 1) {}
    @!Hide[isChecked: true]
    public init(a!: String) { v = 2 }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: true]
public enum B {
    | @!Hide[isChecked: true] AA
    | @!Hide[isChecked: true] BB(Int64)

    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

@!Hide[isChecked: true]
extend A {
    @!Hide[isChecked: true]
    public prop c: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func goo(a: Int64) {}
}

@!Hide[isChecked: true]
public var aaa = 1
@!Hide[isChecked: true]
public func goo(a: Int64) {}

@!Hide[isChecked: true]
public class C<T> {}

public class D<T> {}

public enum E {
    | @!Hide[isChecked: true] AA
    | @!Hide[isChecked: true] BB(Int64)

    @!Hide[isChecked: true]
    public prop b: Int64 { get() {1} }
    @!Hide[isChecked: true]
    public func foo(a: Int64) {}
}

// FILE: override_by_hide6_2.cj



package ohos.dep

import ohos.labels.*

public interface I1 {
    func foo() {}
}

@!Hide
public class A1 <: I1 {
    public func <!HIDE_MISSING_HIDE!>foo<!>() {}
}

@!Hide
public interface I2 {
    func foo() {}
}

public class A2 <: I2 {
    public func foo() {}
}

@!Hide[isChecked: false]
public interface I3 {
    func foo() {}
}

@!Hide[isChecked: true]
public class A3 <: I3 {
    public func <!HIDE_DIFF_PARAM!>foo<!>() {}
}

@!Hide[isChecked: true]
public interface I4 {
    func foo() {}
}

@!Hide[isChecked: false]
public class A4 <: I4 {
    public func <!HIDE_DIFF_PARAM!>foo<!>() {}
}
