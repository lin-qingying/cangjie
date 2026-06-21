

package pkgs0011001a

public struct Test<T> {
    public var member: T
    public init(a: T) {
        member = a
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
