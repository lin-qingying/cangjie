// FILE: missingEllipse_imported.lib.cj


package a

public enum A<T> {
    | B | ...
}

// FILE: missingEllipse_imported.cj



import a.*

main() {
    let a = A<Int64>.B
    <!NON_EXHAUSTIVE_MATCH!>match (a) {
        case B => 0
    }<!>
}
