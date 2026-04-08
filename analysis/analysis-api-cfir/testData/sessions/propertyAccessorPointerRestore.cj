package sample.sessions.accessors

class Holder {
    prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }
}

func readState(holder: Holder): Int64 {
    return holder.state
}
