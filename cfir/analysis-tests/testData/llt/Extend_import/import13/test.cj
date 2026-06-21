// FILE: pkg0.cj


package pkg0

public interface CellUnary {
    operator func ()(input0: Float64): Float64
}

// FILE: pkg1.cj


package pkg1

public import pkg0.*

public struct Conv2d {}

extend Conv2d <: CellUnary {
    public operator func ()(input: Float64): Float64 {
        input
    }
}

// FILE: test.cj



internal import pkg1.*

main(): Int64 {
    let conv = Conv2d()
    let output = conv(1.0)
    return 0
}
