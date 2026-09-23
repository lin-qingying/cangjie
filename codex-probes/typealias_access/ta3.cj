internal class Hidden3 {}

type G = Hidden3

public type H = Array<Hidden3>

public type I = (Hidden3, Int64)

internal type J = Int64

internal func g3<T>(x: T): T where T <: Hidden3 {
    return x
}
