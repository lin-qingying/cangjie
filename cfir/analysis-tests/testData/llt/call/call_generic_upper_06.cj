

interface I {
    func f(x: ToString) {
        1
    }
    func f(x: Int64) {
        2
    }
}

func g<T>(x: T) where T <: I {
    x.f(1)
}

main() {}
