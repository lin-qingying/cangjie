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

// FILE: override_by_hide3_3.cj



package ohos.dep

import ohos.labels.*

public interface I {
    func foo() {}
    func goo() {}
    func koo() {}
}

@!Hide[isChecked: true]
public class C <: I {
    public func <!HIDE_MISSING_HIDE!>foo<!>() {}
    public func <!HIDE_MISSING_HIDE!>goo<!>() {}
    public func <!HIDE_MISSING_HIDE!>koo<!>() {}
}
