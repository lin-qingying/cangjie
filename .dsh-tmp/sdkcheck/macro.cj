import std.deriving.*

@Derive[ToString]
struct Point {
    Point(let x: Int64, let y: Int64) {}
}

main() {
    println(Point(1, 2).toString())
}
