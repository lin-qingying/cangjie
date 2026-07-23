

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
        let _ = A<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    }) |> g<Unit>
}
