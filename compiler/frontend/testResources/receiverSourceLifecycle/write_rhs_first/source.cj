package receiver_write_rhs_first
public class Cell { public var value: Int64 = 0 }
public func probe(flag: Bool): Int64 {
    let x: Int64
    (if (x == 1) { Cell() } else { Cell() }).value = (if (flag) { x = 1; 10 } else { x = 2; 20 })
    return x
}
