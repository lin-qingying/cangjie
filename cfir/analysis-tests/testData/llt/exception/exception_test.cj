

internal import std.core.*

main(): Int64 {
    try {
        throw <!UNRESOLVED_REFERENCE!>g<!>umentException();
    } catch (e: IllegalArgumentException) {
        print("in catch block!")
    } finally {
        print("in finally block!")
    }
    return 0
}
