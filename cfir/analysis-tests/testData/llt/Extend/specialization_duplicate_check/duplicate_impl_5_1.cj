


interface I1<T> {}

interface I2<T> <: I1<T> {}

class A<T> {}

extend A<String> <: I2<String> {}

extend<T> A<T> <: I1<T> {}

main() {}
