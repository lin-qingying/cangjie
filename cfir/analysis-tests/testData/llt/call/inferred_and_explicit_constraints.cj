class Plain {}
interface First {}
interface Second {}

func ordered<T>(value: T): Unit where T <: Comparable<T> {}
func orderedArray<T>(value: Array<T>): Unit where T <: Comparable<T> {}
func both<T>(value: T): Unit where T <: First & Second {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>ordered<!>(Plain())
    ordered<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Plain<!>>(Plain())
    let values: Array<Plain> = [Plain()]
    <!UNABLE_TO_INFER_GENERIC_FUNC!>orderedArray<!>(values)
    orderedArray<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Plain<!>>(values)
    ordered(1)
    ordered<Int64>(1)
    both<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Plain<!>>(Plain())
}
