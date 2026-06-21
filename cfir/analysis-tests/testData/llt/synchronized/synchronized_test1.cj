

internal import std.sync.*

main(): Int64 {
    let a: Int64 = synchronized(ReentrantMutex()) <!TYPE_MISMATCH!>{}<!>
    0
}
