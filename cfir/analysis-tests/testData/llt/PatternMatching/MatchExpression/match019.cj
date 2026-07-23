


let a = true
let b: UInt16 = 5

struct c {
    public let <!CLASS_UNINITIALIZED_FIELD!>d<!>: UInt16
    public func e() {
        match {
            case <!TYPE_MISMATCH!>{=>}<!> => if (true) {
                b
            } else {
                UInt16(2)
            }
            case _ => if (a) {
                d
            } else {
                UInt16(9)
            }
        }
    }
}
