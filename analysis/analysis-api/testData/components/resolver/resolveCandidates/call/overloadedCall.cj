// FILE: overloadedCall.cj
// TARGET_CALL: Point(value: 1)
package sample.resolver.candidates

class Point {
    public let x: Int64
    public const init() {
        x = 0
    }
    public const init(value!: Int64) {
        x = value
    }
}

func makeOrigin(): Point {
    return Point()
}

func makePoint(): Point {
    return Point(value: 1)
}
