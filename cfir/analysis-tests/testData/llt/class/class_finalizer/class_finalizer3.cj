


// parse diag
// class A {
//      ~init(a:Int32){}
// }

class B {
    ~init<!CANNOT_CURRYING!>(<!>)() {}
}

class C {
    ~init() {
        return <!RETURN_TYPE_MISMATCH!>0<!>
    }
}

class D {
    ~init() {}
}

class F<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: String {
    <!FORBID_GENERIC_FINALIZER!>~<!>init<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: String {}
}

main() {
    0
}
