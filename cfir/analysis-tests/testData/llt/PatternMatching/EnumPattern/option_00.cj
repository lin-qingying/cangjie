

enum Foo {
    | Some(Int64)

    prop value: Int64 {
        get() {
            match (this) {
                case Some(value_) => value_
            }
        }
    }
}

main(): Unit {
    let a: Option<Foo> = Option.Some(Foo.Some(10))
    let valueA = a?.value
}
