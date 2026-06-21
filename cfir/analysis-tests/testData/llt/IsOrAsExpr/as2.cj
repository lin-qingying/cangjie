


interface I<T> {}

extend<T> Option<T> <: I<T> {}

let x: I<Bool> = true as Bool

main() {}
