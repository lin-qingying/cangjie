

package pkg

public enum ET<T> {
    A | B | C

    public func bar(a: T) {
        return a
    }
    public static func f<U>(a: T, b: U): T {
        return a
    }
}
