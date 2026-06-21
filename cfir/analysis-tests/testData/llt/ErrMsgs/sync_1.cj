


internal import std.sync.*

main() {
    let b = synchronized(<!TYPE_MISMATCH!>()<!>) {}
    let c: Int32 = synchronized(<!TYPE_MISMATCH!>false<!>) {
        1;
        <!TYPE_MISMATCH!>true<!>
    } // should raise two error messages: one for the lock object and one for the sync body.

    let mtx: <!DEPRECATED_WARNING!>ReentrantMutex<!> = <!DEPRECATED_WARNING!>ReentrantMutex<!>()
    let x: Int64 = synchronized(mtx) {
        <!TYPE_MISMATCH!>true<!>
    }
    x + 2 // should not raise an error
    c >> 3 // should not raise an error
    0
}
