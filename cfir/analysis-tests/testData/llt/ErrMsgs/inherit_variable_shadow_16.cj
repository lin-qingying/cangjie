

main(): Int64 {
    var MyChild = Child()
    if (MyChild.a != 2) {
        return 1
    }
    if (Son.a != 3) {
        return 1
    }
    return 0
}

open class Father {
    private static var a = 1
}

class Child <: Father {
    public var a = 2
}

class Son <: Father {
    public static var a = <!UNRESOLVED_REFERENCE!>H<!>
}
