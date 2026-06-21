

class X {}

class Y {}

class A<T, K> where T <: X, K <: Y {}

extend<<!CONFLICTING_UPPER_BOUNDS!>T<!>> A<T, T> {}

main() {
}
