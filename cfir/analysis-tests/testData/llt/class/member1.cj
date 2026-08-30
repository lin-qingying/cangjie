

class A {
    func a() {}
    var c = this.<!ILLEGAL_USAGE_OF_MEMBER!>a<!>
}
