


struct Foo<T> where T <: ToString {}

type Foobar<R> = (R) -> Foo<R>

main() {
}
