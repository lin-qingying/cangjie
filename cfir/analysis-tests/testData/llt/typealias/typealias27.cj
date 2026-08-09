


struct Foo<T> where T <: ToString {}

type Foobar<R> = Foo<R>

struct S {}

main() {
    let a = Foobar<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>S<!>>()
}
