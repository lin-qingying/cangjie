

var a = B.<!USED_BEFORE_INITIALIZATION!>a<!>
var b = A.<!USED_BEFORE_INITIALIZATION!>b<!>
var d = 1

class B {
    public static var a: Int64 = A.<!USED_BEFORE_INITIALIZATION!>b<!>
    public static var c = d
}

class A {
    public static var b: Int64 = B.a
}
