

class A {
    func a() {}
    var c = this.<!USED_BEFORE_INITIALIZATION!>a<!>
}
