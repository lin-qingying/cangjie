

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res1 = Data().Poo()
    var res2 = Data().Qoo()
    if (res1 == 4 && res2 == 6) {
        return 0
    }
    return 1
}

class Data <: Base {
    public func Poo(a!: Int64 = 4) {
        return a
    }

    public func Qoo(a!: Int64 = 6) {
        return a
    }
}

abstract class Base {
    public func Poo(<!CANNOT_HAVE_DEFAULT_PARAM!>a!: Int64 = 2<!>): Int64
    public func Qoo(<!CANNOT_HAVE_DEFAULT_PARAM!>a!: Int64 = 3<!>): Int64
}
