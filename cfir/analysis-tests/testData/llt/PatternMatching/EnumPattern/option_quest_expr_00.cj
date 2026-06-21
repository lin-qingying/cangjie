

package core

struct Foo {
    let foo: Int64 = 17
}

enum Opt1on<T> {
    | Some(T)
}

main() {
    let x = Option.Some(Foo())
    let t = x?.foo
}
