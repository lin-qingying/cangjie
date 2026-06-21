

struct Foo {
    let foo: Int64 = 17
}

main() {
    let x = Some(Foo())
    let t = x?.foo
}
