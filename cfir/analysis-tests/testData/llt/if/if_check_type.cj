

main() {
    var v: Int64 = try {
        1
    } catch (e: Exception) {
        <!TYPE_MISMATCH!>if (true) {}<!>
    }
}
