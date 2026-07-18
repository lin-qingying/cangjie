

open class Father {}

class Child <: Father {}

interface J {
    func foo(a: Int16): Child {
        return Child()
    }
}

public class B <: J {
    public override func <!RETURN_TYPE_INCOMPATIBLE!>foo<!>(a: Int16): Father {
        return Child()
    }
}
