// RUN_PIPELINE_TILL: FRONTEND
// unsafe 块与 foreign 调用：foreign 函数声明、unsafe 内调用、unsafe 内嵌套普通调用、CPointer / CString 形参。
// 官方依据：Cangjie 文档《C 互操作》《unsafe》；cjc 1.0.5 / 1.1.3 零诊断。
foreign func nativeAbs(x: Int64): Int64
foreign func nativeSum(p: CPointer<Int64>, n: Int64): Int64

class UsesNative {
    var v: Int64 = 0

    func abs(): Int64 {
        unsafe {
            return nativeAbs(v)
        }
    }

    func nested(): Int64 {
        unsafe {
            let inner = nativeAbs(v)
            return inner + 1
        }
    }
}

func cfuncSignature(p: CPointer<Int64>, s: CString): Unit {
}
