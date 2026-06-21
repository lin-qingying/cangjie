


var v = 0

class Person <: Resource {
    var status = false
    public var name: String = "Shunshun"
    public init() {
        v++
    }
    public func isClosed(): Bool {
        v++
        var res = status
        status = true
        return res
    }
    public func close(): Unit {
        v++
    }
}

main(): Int64 {
    try (a = Person()) {
        v++
    } finally {
        v++
    }
    if (v != 5) {
        println(v)
        return 1
    }
    return 0
}
