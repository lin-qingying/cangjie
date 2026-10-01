// RUN_PIPELINE_TILL: FRONTEND
class Vec {
    private let data: Array<Int64> = [0, 0]

    operator func [](index: Int64): Int64 {
        return this.data[index]
    }

    operator func [](index: Int64, value!: Int64): Unit {
        this.data[index] = value
    }

    operator func +(other: Vec): Vec {
        return Vec()
    }

    operator func -(other: Int64): Int64 {
        return other
    }

    operator func -(): Vec {
        return Vec()
    }
}

struct Point {}

extend Point {
    operator func +(other: Point): Point {
        return Point()
    }
}
