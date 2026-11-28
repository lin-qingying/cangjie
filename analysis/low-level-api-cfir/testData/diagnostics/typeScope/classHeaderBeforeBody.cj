interface Bound {}
class Box<T> where T <: Bound {
    var Bound: Int64 = 0
    init() {}
}
