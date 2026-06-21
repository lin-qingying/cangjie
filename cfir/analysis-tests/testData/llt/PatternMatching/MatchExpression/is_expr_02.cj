

interface I {}

struct Foo <: I {
    Foo() {
        println("Foo")
    }
}

main() {
    println(1 is Int64)
    println(1 is Any)
    println(Foo() is Foo)
    println(Foo() is I)
    println(None<Bool> is Option<Bool>)
}
