

package pkg

public enum ET {
    A | B | C(Int64)

    public func bar(a: Int64) {
        return a
    }
    public static func f(a: Int64): Int64 {
        return a
    }
}
