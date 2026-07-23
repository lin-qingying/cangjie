// FILE: missingEllipse_imported.lib.cj


package a

public enum A<T> {
    | B | ...
}

// FILE: missingEllipse_imported.cj



import a.*

main() {
    let a = A<Int64>.B
    match (<!NON_EXHAUSTIVE_MATCH!>a<!>) {
        case B => 0
    }
}
