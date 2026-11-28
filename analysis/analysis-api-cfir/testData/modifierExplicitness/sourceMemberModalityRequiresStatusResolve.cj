interface Contract {
    func required(): Unit
    func provided(): Unit {}
}

abstract class Host {
    func pending(): Unit
}

open class PropertyHost {
    public open func sourceMarked(): Unit {}
    public open prop value: Int32 {
        get() { return 1 }
    }
}
