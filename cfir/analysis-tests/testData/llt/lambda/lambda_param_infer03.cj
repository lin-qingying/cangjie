


func foo<T>(x: Int, f: (Int) -> T) {
    f(x)
}

main() {
    foo(1, {_ => true})
    ()
}
