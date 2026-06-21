// FILE: pkg.cj


package pkg

public interface aa {
    prop hi: String
}

public open class bb {
    public prop hi: String {
        get() {
            hiStr
        }
    }

    var hiStr: String = "hi"

    public init(s: String) {
        hiStr = s
    }
}

public open class cc <: bb & aa {
    public init(s: String) {
        super(s)
    }
}

// FILE: main.cj



internal import pkg.*

class dd <: cc {
    init() {
        super("dd")
    }

    public func SayHi() {
        return hi // ok
    }
}

main(): Int64 {
    let str = dd().SayHi()
    if (str != "dd") {
        return 1
    }
    return 0
}
