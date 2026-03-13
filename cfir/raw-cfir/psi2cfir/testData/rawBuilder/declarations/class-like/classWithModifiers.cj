abstract class Shape {
    abstract func area(): Float64
}

sealed class Color {
}

class Counter {
    static var count: Int64 = 0

    static func getCount(): Int64 {
        return count
    }
}
