package probe

func arraySizeStubs(): Int64 {
    let bytes: Array<Byte> = [1u8, 2u8]
    let asCollection: Collection<Byte> = bytes
    let n = bytes.size
    if (bytes.isEmpty()) {
        return n
    }
    return asCollection.size + n
}
