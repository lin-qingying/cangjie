// RUN_PIPELINE_TILL: FRONTEND
class Box<T> {
    let value: T

    init(value: T) {
        this.value = value
    }

    func get(): T {
        return this.value
    }
}

extend<T> Box<T> where T <: ToString {
    func dump(): Unit {
        let _ = this.get().toString()
    }
}
