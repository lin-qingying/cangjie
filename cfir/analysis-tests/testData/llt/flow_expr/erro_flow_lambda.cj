

public open class A {}

func f(x: Int64): Int64 {
    1
}

main() {
    var d: Bool = 0 |> {
        f: Int64 =>
        <!TOO_MANY_ARGUMENTS!>A(name: 1)<!>;
        A()
    }
    var g = f ~> {
        a: Int64 =>
        <!TOO_MANY_ARGUMENTS!>A(name: 1)<!>;
        return 0
    }
}
