

package pkg

public enum ET {
    A | B | C

    public func bar<T>(a: T) {
        return a
    }
    public static func f<T>(a: T): T {
        return a
    }
}
