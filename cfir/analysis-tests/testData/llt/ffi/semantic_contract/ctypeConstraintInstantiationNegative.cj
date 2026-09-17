// 官方 CheckGenericDeclInstantiation 只在实例化检查中排除实际实参 CType。
class Holder<T> where T <: CType {
    public init() {}
}

func bounded<T>(): Unit where T <: CType {}

main() {
    let holder = Holder<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>CType<!>>()
    bounded<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>CType<!>>()
    let pointer = CPointer<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>CType<!>>()
    0
}
