package cjmp_pointer_extend

specific extend<U> CPointer<U> <: Contains<U> {}

main() {
    let pointer = CPointer<Int64>()
    pointer.contains(1)
    0
}
