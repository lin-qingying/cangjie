

internal import std.sync.*

main(): Int64 {
    let a: Int64 = synchronized(<!DEPRECATED_WARNING!>ReentrantMutex<!>()) <!TYPE_MISMATCH!>{}<!>
    0
}
