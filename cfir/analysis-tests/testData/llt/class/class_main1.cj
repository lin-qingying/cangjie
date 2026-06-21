


open class Parent {
    protected var field: Int32 = 1
}

class Child <: Parent {
    public func main1() {
        var child: Child = Child()
        field = 2
        if (child.field != 1) {
            return 1
        }
        return 0
    }
}

<!UNEXPECTED_PARAM_FOR_ENTRY!>m<!>ain(a: Int32): Int64 {
    return Child().main1()
}
