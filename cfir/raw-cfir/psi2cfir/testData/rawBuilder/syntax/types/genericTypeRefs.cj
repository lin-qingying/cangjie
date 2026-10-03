// RUN_PIPELINE_TILL: FRONTEND
// 泛型类型引用：单形参 / 多形参、嵌套泛型、类型别名作实参、泛型接口作实参、数组的元素类型为泛型、类型形参本身作为实参。
// 官方依据：Cangjie 文档《泛型》；cjc 1.0.5 / 1.1.3 零诊断（VArray 的元素类型必须是数值类型，接口类型非法，见 recovery/varrayRefTypeArg.cj）。
interface Box<T> {
    func value(): T
}

type Alias<T> = Box<T>

func nested(a: Box<Box<Box<Int64>>>): Unit { }
func multi(a: Box<Box<String>>): Unit { }
func aliasArg(a: Alias<Int64>): Unit { }
func arrayOfGeneric(a: Array<Box<Int64>>): Unit { }
func tupleOfGeneric(a: (Box<Int64>, Array<String>)): Unit { }
func varrayOfGeneric(a: VArray<Int64, $2>): Unit { }

func genericPassThrough<T>(x: T): T {
    return x
}

func typeParamAsArg(a: Box<Int64>): Int64 {
    let f: (Box<Int64>) -> Int64 = { b: Box<Int64> => 1 }
    return f(a) + genericPassThrough<Int64>(1)
}
