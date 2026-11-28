// 无体声明（三）：顶层函数、顶层变量、常量与类型别名的声明文件形态。
// 官方依据：Cangjie 文档《函数定义》《变量》《类型别名》；docs/cjd-declaration-file-support-v3.md。
func compute(a: Int64, b: Int64): Int64

func noReturn(a: Int64)

let PI: Float64
var counter: Int64
const LIMIT: Int64

type Handler = (Int64) -> Int64
type Pair<A, B> = (A, B)

foreign func puts(s: CString): Int32
