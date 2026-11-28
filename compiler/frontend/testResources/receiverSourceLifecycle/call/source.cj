package receiver_call
public class Cell {
    public var value: Int64 = 0
    public func use(a: Unit, b: Unit): Unit {}
}
public func probe(flag: Bool): Int64 {
    let x: Int64
    (if (flag) { x = 1; Cell() } else { x = 2; Cell() }).use((), ())
    return x
}
