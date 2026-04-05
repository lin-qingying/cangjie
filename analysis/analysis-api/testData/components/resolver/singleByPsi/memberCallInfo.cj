// FILE: memberCallInfo.cj
// TARGET_CALL: counter.add(42)
// EXPECTED_CALLABLE_NAME: add
// EXPECTED_EXPLICIT_RECEIVER_TYPE: sample.Counter
// EXPECTED_ARGUMENT_TYPE: Int64

package sample

class Counter {
    public func add(value: Int64): Int64 {
        return value
    }
}

func buildCounter(): Counter {
    return Counter()
}

func useCounter(): Int64 {
    let counter = buildCounter()
    return counter.add(42)
}
