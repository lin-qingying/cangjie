


struct R<o, j> {
    let n: (Range<Int16>) = 127..7701
}

main() {
    try {
        func k<o>() {
            let x: R<Int16, Range<Int32>>
            var l: Range<UInt64> = 127..155
        }
        k<Option<Int32>>()
    } catch (e: NoneValueException) {
    }
}
