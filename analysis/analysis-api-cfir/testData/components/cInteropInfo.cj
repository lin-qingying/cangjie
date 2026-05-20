package cfir.components

@C
struct NativeBox {
}

@FastNative
@ForeignName["native_sum"]
@CallingConv[CDECL]
foreign func nativeSum(left: Int64, right: Int64): Int64
