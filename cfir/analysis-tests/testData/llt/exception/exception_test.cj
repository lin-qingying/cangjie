

internal import std.core.*

main(): Int64 {
    try {
        throw <!UNRESOLVED_REFERENCE!>gumentException<!>();
    } catch (e: IllegalArgumentException) {
        print("in catch block!")
    } finally {
        print("in finally block!")
    }
    return 0
}
