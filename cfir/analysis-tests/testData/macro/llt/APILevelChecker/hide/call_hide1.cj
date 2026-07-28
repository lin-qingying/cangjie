// FILE: hide.cj


package ohos.labels

@Annotation
protected class <!ANNOTATION_NON_PUBLIC!>Hide<!> {
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

// FILE: call_hide1.cj




import ohos.dep.*
import ohos.dep

class D<T> <: <!UNRESOLVED_REFERENCE!>A<!> where T <: <!UNRESOLVED_REFERENCE!>A<!> {
    func xxx<R>(d: <!UNRESOLVED_REFERENCE!>A<!>): <!UNRESOLVED_REFERENCE!>A<!> where R <: <!UNRESOLVED_REFERENCE!>A<!> {
        var a: <!UNRESOLVED_REFERENCE!>A<!> = <!UNRESOLVED_REFERENCE!>A()<!>
        <!UNRESOLVED_REFERENCE!>B.AA<!>
        <!UNRESOLVED_REFERENCE!>B.BB(1)<!>
        <!UNRESOLVED_REFERENCE!>E.AA<!>
        <!UNRESOLVED_REFERENCE!>E.BB(1)<!>
        <!UNRESOLVED_REFERENCE!>goo(1)<!>
        var b: D<<!UNRESOLVED_REFERENCE!>A<!>>
        var c: <!UNRESOLVED_REFERENCE!>dep.A<!>
        a is <!UNRESOLVED_REFERENCE!>A<!>
        a as <!UNRESOLVED_REFERENCE!>A<!>
        <!UNRESOLVED_REFERENCE!>aaa<!> // hiden global var
        a
    }
}

main() {}
