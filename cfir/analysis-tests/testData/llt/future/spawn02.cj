


interface I<T> {}

extend<T> Future<T> <: I<T> {}

main() {
    let i: I<String> = spawn {=> "Hello"}
}
