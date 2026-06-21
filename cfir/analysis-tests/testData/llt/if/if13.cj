


internal import std.sync.*
internal import std.collection.*

func foo() {
    /* all cases where discarded-value state is transitive */
    if (true) {
    } else if (true) {
    } else {
        (match (true) {
            case _ =>
                var mtx: ReentrantMutex = ReentrantMutex()
                (((((synchronized(mtx) {
                    try {
                        "1"
                    } catch (_) {
                        if (true) {
                            1
                        } else {
                            1.0
                        }
                    }
                })))))
        })
    }
    0
}

main() {
    return foo()
}
