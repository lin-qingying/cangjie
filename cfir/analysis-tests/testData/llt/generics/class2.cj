

open class Father {}

class Child <: Father {}

interface J {
    func foo(a: Int16): Child {
        return Child()
    }
}

public class B <: J {
    public override func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(a: Int16): Father {
        return Child()
    }
}
