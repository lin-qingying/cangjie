


public interface Cloneable {
    func clone(): Cloneable
}

public interface LiteralValue <: Cloneable {
    func <!RETURN_TYPE_INVARIANCE!>clone<!>(): LiteralValue
}

extend String <: LiteralValue {}

main() {}
