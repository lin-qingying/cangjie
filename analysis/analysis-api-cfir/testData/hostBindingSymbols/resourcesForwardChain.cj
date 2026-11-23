package test.hosts

class ManagedResource <: Resource {
    public func isClosed(): Bool { true }
    public func close(): Unit {}
    public func value(): Int64 { 1 }
}

func useForwardChain(): Unit {
    try (a = <caret_toMiddle>b, b = <caret_toLast>c, c = ManagedResource()) {
        let one: Int64 = <caret_firstUse>a.<caret_firstValue>value()
        let two: Int64 = <caret_middleUse>b.value()
        let three: Int64 = <caret_lastUse>c.value()
        println(one + two + three)
    }
}
