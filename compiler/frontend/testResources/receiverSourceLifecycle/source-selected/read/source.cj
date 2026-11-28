package receiver_read
public class Cell {
    public var value: Int64 = 0
    public func use(a: Unit, b: Unit): Unit {}
}
@Frozen
public func probe(flag: Bool): Int64 {
    let x: Int64
    let y = (if (flag) { x = 1; Cell() } else { x = 2; Cell() }).value
    return x + y
}
