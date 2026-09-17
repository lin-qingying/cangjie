// ConstEvaluationChecker.cpp:179–190 对 ConstSafe 声明跳过整个常量检查子树。
// 普通名称/类型检查仍执行；模块身份取自官方 Parser.cpp 的 package 前缀。
package std.cfir_annotation_scopes

func runtimeValue(): Int64 { 1 }

@ConstSafe
const globalValue: Int64 = runtimeValue()

@ConstSafe
const func uncheckedFunction(): Int64 { runtimeValue() }

class ConstructorScope {
    @ConstSafe
    public const init() { runtimeValue() }
}

@ConstSafe
class ClassScope {
    public let stored: Int64 = runtimeValue()
    public const init() {}
    public const func read(): Int64 { runtimeValue() }
}

func localScope(): Int64 {
    @ConstSafe
    const localValue: Int64 = runtimeValue()
    @ConstSafe
    const func uncheckedLocal(): Int64 { runtimeValue() }
    localValue + uncheckedLocal()
}

main() {}
