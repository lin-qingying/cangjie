

class C <: ToString {
    public var x: Int32 = 999
    public func toString() {
        return x.toString()
    }
}

enum E<T> where T <: ToString {
    A | B(T) | ...
}

func f<T>(e: E<T>) where T <: ToString {
    match (e) {
        case A => println("AAA")
        case B(c) =>
            print("BBB");
            println(c)
        case _ => println("unknown")
    }
}

main() {
    f(E<C>.B(C()))
    return 0
}
