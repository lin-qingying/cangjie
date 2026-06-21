


internal import std.sync.*

var arr: Array<Int64> = Array<Int64>(10, repeat: 10)
let lock = <!DEPRECATED_WARNING!>ReentrantMutex<!>()

func foo() {
    synchronized(lock) {
        for (i in arr) {
            println(i)
        }
    }
}

main() {
    spawn {
        foo()
    }
    return 0
}
