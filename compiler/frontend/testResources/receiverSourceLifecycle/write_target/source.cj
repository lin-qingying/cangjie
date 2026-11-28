package receiver_write_target
public class Cell { public var value: Int64 = 0 }
public func select(n: Int64): Cell { Cell() }
public func probe(flag: Bool): Int64 {
    let x: Int64
    select(x).value = (if (flag) { x = 1; 10 } else { x = 2; 20 })
    return x
}
