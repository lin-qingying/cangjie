foreign func malloc(size: UIntNative): CPointer<Unit>

main() {
    var node17 = CPointer(malloc)
    var node9: CType = CPointer()
    var node14: CString = CPointer()
    var node15 = CPointer()
    var node13 = CPointer(CPointer<Unit>())
    let erased: CType = CPointer()
    let erasedCast: CType = CPointer(CPointer<Int64>())
}
