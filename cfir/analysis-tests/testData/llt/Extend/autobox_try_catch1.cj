// WITH_EXTRA_CHECKERS

interface I {
    func f() {
        println("In f of interface!");
        return 1
    }
}

extend Int64 <: I {
    public func f() {
        println("In f of extend Int64!");
        return 1
    }
}

func g(): I {
    let <!UNUSED_VARIABLE!>y<!>: I = 9
    try {
        return 2
    } catch (e: Exception) {
        println("Exception caught here")
        y
    } finally {
        return 11
    }

    return 0
}

main() {
    let x = g()

    x.f()
    return 0
}
