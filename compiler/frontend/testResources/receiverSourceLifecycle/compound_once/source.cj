package receiver_compound_once
public class Cell { public var value: Int64 = 0 }
public func select(cell: Cell): Cell { cell }
public func probe(flag: Bool): Int64 {
    let x: Int64
    select(if (flag) { x = 1; Cell() } else { x = 2; Cell() }).value += 1
    return x
}
