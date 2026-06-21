

internal import std.ast.*

main() {
    let x: String = if (true) {
        "a"
    } else {
        <!TYPE_MISMATCH!>quote(1)<!>
    }
}
