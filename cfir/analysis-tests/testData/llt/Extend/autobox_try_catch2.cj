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

func g() {
    let y: I = 2
    let x: I = try {
        y
    } catch (e: Exception) {
        println("Exception caught here")
        9
    } finally {
        <!UNUSED_EXPRESSION!>11<!>
    }
    x.f()
    return 0
}

main() {
    g()
    return 0
}
