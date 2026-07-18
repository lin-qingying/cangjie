


interface I1 {
    func foo(): Int64
}

interface I3 {
    func goo(): I1
}

class CA<T> <: I3 where T <: I1 {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func <!RETURN_TYPE_INVARIANCE!>goo<!>(): T { // '()->T' cannot override '()->I1' even if 'T <: I1', function signature mast be '()->I1'.
        println("here")
        x
    }
}

struct TEST_1 <: I1 {
    public func foo(): Int64 {
        0
    }
}

main() {
    0
}
