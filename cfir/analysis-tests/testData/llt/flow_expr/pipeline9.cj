

class A <: Resource {
    public func isClosed() {
        true
    }
    public func close() {}
}

func f() {}

func f(_: Unit) {}

func g<T>(_: Unit) {}

main() {
    f(try (x = A()) {} finally {
        let _ = <!TOO_MANY_ARGUMENTS!>A(1)<!>
    }) |> g<Unit>
}
