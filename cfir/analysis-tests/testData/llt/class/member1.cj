

class A {
    func a() {}
    var c = <!USED_BEFORE_INITIALIZATION!>this.a<!>
}
