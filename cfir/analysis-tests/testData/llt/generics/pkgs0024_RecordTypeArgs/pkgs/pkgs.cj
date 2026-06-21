

package pkgs

// Record
public struct Data<T> {
    public let width: T
    public let length: T
    public init(width1: T, length1: T) {
        width = width1
        length = length1
    }
    public func area(): T {
        return width
    }
}

public let data: Data<Int32> = Data<Int32>(3, 4)
