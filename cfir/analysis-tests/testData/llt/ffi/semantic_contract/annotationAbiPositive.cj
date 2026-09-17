// 官方 CFFICheck.cpp：调用约定、FastNative、Frozen 分别保留各自声明约束。
@CallingConv[CDECL]
foreign func cdeclForeign(value: Int64): Int64

@FastNative
foreign func fastForeign(): Unit

@C
@CallingConv[STDCALL]
func stdcallSource(value: Int64): Int64 { value }

@Frozen
func frozenSource(): Int64 { 1 }

class FrozenMembers {
    @Frozen
    func value(): Int64 { 1 }

    @Frozen
    prop result: Int64 { get() { 1 } }
}

main() { 0 }
