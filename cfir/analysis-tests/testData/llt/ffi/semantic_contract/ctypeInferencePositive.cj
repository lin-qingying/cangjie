// CType 约束保留具体输入与外层类型变量；不能扩大成禁止接口上界或显式 CType 实参。
interface Marker {}
struct OrdinaryBound<T> where T <: Marker { public init() {} }
struct Unbounded<T> { public init() {} }
struct NoValue<T> where T <: CType { public init() {} }

struct WithValue<T> where T <: CType {
    public let value: T
    public init(value: T) { this.value = value }
}

func makeNoValue<T>(): NoValue<T> where T <: CType { NoValue<T>() }
func makeValue<T>(): T where T <: CType { throw Exception("not executed") }
func identity<T>(value: T): T where T <: CType { value }
func plainIdentity<T>(value: T): T { value }

// CType 本身仍可正常传递，也可显式传给没有 CType 上界的泛型函数。
func preserve(value: CType): CType { value }
func explicitCTypeIdentity(value: CType): CType { plainIdentity<CType>(value) }

func forward<U>(value: U): U where U <: CType { identity(value) }
func contextual<U>(): NoValue<U> where U <: CType { NoValue() }

main() {
    let expectedConstructor: NoValue<Int32> = NoValue()
    let expectedFactory: NoValue<Int64> = makeNoValue()
    let expectedValue: Int32 = makeValue()

    let constructorInput = WithValue(Int16(1))
    let constructorCheck: WithValue<Int16> = constructorInput
    let functionInput = identity(Int32(1))
    let functionCheck: Int32 = functionInput
    let pointerInput = identity(CPointer<UInt8>())
    let pointerCheck: CPointer<UInt8> = pointerInput
    let expectedPointer: NoValue<CPointer<Int32>> = NoValue()

    let outerInput = forward(UInt16(1))
    let outerCheck: UInt16 = outerInput
    let outerExpected: NoValue<UInt32> = contextual()

    let ordinary = OrdinaryBound()
    let ordinaryCheck: OrdinaryBound<Marker> = ordinary
    let explicitUnbounded = Unbounded<CType>()
    0
}
