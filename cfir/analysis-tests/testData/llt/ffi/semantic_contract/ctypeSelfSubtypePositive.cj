// CType 仍是普通接口；不能为拒绝泛型实例化而破坏它的自反子类型关系。
func sameType(value: CType): CType { value }

func bounded<T>(): Unit where T <: CType {}

func forward<T>(value: T): T where T <: CType {
    bounded<T>()
    value
}

main() { 0 }
