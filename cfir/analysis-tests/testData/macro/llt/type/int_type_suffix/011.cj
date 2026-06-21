


internal import std.ast.*

main() {
    var a = quote(b'A')
    if (a[0].kind.toString() != "CHAR_BYTE_LITERAL") {
        return 1
    }
    return 0
}
