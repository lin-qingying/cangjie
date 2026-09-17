// 官方 IsValidSolution 拒绝隐式选择 CType：声明上界、实参和期望类型均不能使其成为解。
struct NoValue<T> where T <: CType { public init() {} }
func makeNoValue<T>(): NoValue<T> where T <: CType { NoValue<T>() }
func makeValue<T>(): T where T <: CType { throw Exception("not executed") }
func constraintOnly<T>(): Int64 where T <: CType { 0 }
func identity<T>(value: T): T where T <: CType { value }
func plainIdentity<T>(value: T): T { value }

func withCTypeValue(value: CType): CType {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>identity<!>(value)
}

func inferFromCType(value: CType): CType {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>plainIdentity<!>(value)
}

main() {
    let constructor = <!UNABLE_TO_INFER_GENERIC_FUNC!>NoValue<!>()
    let factory = <!UNABLE_TO_INFER_GENERIC_FUNC!>makeNoValue<!>()
    let value = <!UNABLE_TO_INFER_GENERIC_FUNC!>makeValue<!>()
    let unrelatedExpected: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>constraintOnly<!>()
    let handle = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointerHandle<!>()
    let expectedCType: CType = <!UNABLE_TO_INFER_GENERIC_FUNC!>makeValue<!>()
    0
}
