


// Test for generic instantiation correctness
type BytesSlice = Array<Byte>

main() {
    let a: Array<(String, BytesSlice)> = []
    let b: Array<(String, Array<(String, (BytesSlice) -> Unit)>)> = []
}
