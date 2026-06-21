// FILE: a.cj


package a

public open class Remote {
    private var remote_call: Bool

    protected init() {
        remote_call = true
    }

    public prop remoteCall: Bool {
        get() {
            remote_call
        }
    }
}

// FILE: b.cj



package b

import a.*

class B <: Remote {
    public static func ctor() {
        Remote()
    }
}

main() {
    if (B.ctor().remoteCall) {
        0
    } else {
        1
    }
}
