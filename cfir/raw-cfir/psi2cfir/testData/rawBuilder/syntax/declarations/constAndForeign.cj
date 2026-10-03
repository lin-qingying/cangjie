// RUN_PIPELINE_TILL: FRONTEND
// 顶层 const 变量与 const func、foreign func 声明与调用（unsafe 块内）、@Overflow* 属性宏标注函数。
// 官方依据：Cangjie 文档《常量》《函数定义》《注解》；cjc 1.0.5 / 1.1.3 零诊断。
const MAX: Int64 = 100
const NAME: String = "name"

public const func doubled(x: Int64): Int64 {
    return x * 2
}

foreign func absNative(x: Int64): Int64

@OverflowWrapping
func wrappingAdd(a: UInt8, b: UInt8): UInt8 {
    return a + b
}

@OverflowThrowing
func throwingAdd(a: UInt8, b: UInt8): UInt8 {
    return a + b
}

@OverflowSaturating
func saturatingAdd(a: UInt8, b: UInt8): UInt8 {
    return a + b
}

func foreignUses(): Int64 {
    unsafe {
        return absNative(-1)
    }
}
