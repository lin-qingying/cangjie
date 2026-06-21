

main(): Int64 {
    var a = Coo()
    if (a.b3 != 1) {
        return 1
    }
    return 0
}

open class Boo {}

class Coo <: Boo {
    public static var b5: Int64 = 1
    public var b3 = {=> <!UNRESOLVED_REFERENCE!>b0<!>}()
}
