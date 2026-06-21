


public interface Cloneable {
    func clone(): Cloneable
}

public interface LiteralValue <: Cloneable {
    func clone(): LiteralValue
}

extend String <: LiteralValue {}

main() {}
