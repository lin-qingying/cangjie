interface Bound {}
class Box<T> where T <: Bound {
    var Bound: Int64 = 0
    func outer(): Unit {
        func inner(x: Bound): Unit {}
    }
}
