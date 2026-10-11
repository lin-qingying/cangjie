// RUN_PIPELINE_TILL: FRONTEND
class Box<T> {
    private var value: ?T = None

    public func get(): ?T {
        return this.value
    }

    public func put(item: T): Unit {
        this.value = Some(item)
    }
}
