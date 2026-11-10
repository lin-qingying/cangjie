package e4probe

class Holder {
    private let m: Mutex

    public init(m: Mutex) {
        this.m = m
    }

    public func getMutex(): Mutex {
        return m
    }
}

main() {
    let b = synchronized(()) {}
    let c = synchronized(false) {}
    let _ = b
    let _ = c
}
