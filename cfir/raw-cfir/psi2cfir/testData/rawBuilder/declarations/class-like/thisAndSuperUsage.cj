// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    var v: Int64 = 0

    public open func get(): Int64 {
        return 0
    }
}

class Child <: Base {
    var w: Int64 = 0

    func read(): Int64 {
        return this.w + super.get() + this.v
    }
}
