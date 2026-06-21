

package core

struct Foo {
    let foo: Int64 = 17
}

enum Option<T> {
    | Some(T)
}

main() {
    let x: ?Foo = Foo()
    let t = x?.foo
}
