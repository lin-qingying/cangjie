// foreign block 中的函数必须保留文件归属，并在两条 Raw CFIR 路径传播 foreign 状态。
@CallingConv[STDCALL]
@FastNative
foreign {
    func readNative(value: CPointer<Int32>): Int32
    func writeNative(value: CPointer<Int32>, replacement: Int32): Unit
}

unsafe main() {
    let address = CPointer<Int32>()
    let value = readNative(address)
    writeNative(address, value)
    0
}
