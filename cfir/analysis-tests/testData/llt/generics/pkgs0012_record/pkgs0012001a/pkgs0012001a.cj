

package pkgs0012001a

public struct Test<T> {
    public let member1: Int32
    public var member2: T
    public init(a: Int32, b: T) {
        member1 = a
        member2 = b
    }
    public func testNormal(a: Bool): Bool {
        return a
    }
    public func testGeneric1(t: T): T {
        return t
    }
    public func testGeneric2(t: T, j: Bool): Bool {
        return j
    }
}
