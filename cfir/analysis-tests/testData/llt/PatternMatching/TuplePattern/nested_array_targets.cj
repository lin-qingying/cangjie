public func nestedVArray() {
    let value = VArray<VArray<VArray<VArray<Int32, $2>, $2>, $2>, $2>(
        repeat: [[[1, 2], [1, 2]], [[1, 2], [1, 2]]]
    )
}
public func nestedTupleVArray() {
    let value = VArray<VArray<(Int32, Rune), $2>, $2>(repeat: [(1, r'a'), (2, r'b')])
}
func acceptFunctions(values: Array<() -> Int64>): Int64 { values[0]() }
public func functionArrayArgument(): Int64 { acceptFunctions([{ => 1 }, { => 2 }]) }
