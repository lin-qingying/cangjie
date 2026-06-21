


internal import std.core.Object

public open class Monkey {
    protected open func say() {
    }
}

public type GoodMonkey = Monkey

public open class LittleMonkey <: GoodMonkey {
    protected override func say() {
    }
}

main() {
    return 0
}
