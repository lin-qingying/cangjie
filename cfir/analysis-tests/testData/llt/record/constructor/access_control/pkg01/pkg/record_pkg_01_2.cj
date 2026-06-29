

package pkg

let n: Int64 = <!NO_CONSTRUCTOR!>A<!>().a

struct B {
    public let a: Int64 = n
    public let b: Int64 = <!ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR!>this<!>.a
    public var x: Int64 = 1
    public let g: () -> Int64 = {=> return this.x * this.x}
    public func h() {
        return this.a
    }
}
