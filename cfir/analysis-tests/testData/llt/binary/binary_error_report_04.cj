

// Test for error reporting was not able to report error in main's binaryExpr
interface I<T> {}

func foo(): Unit {
    try {
        try {
            ();
        } finally {
            ((UInt64(160) & 127)..(UInt64(61) & 127) : ((-85) | 1))
        }
    } catch (_) {
    }
    ()
}

func test<T>(): UInt64 {
    1
}

main() {
    1 ** test<<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>I<!>>()
    return 0
}
