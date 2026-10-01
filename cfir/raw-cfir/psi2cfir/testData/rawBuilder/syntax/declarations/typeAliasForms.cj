// RUN_PIPELINE_TILL: FRONTEND
// 类型别名：普通别名、泛型别名、函数类型别名（含具名形参——参数列表要么全具名要么全无名）、元组别名、可选别名、嵌套泛型别名、别名引用别名。
// 官方依据：Cangjie 文档《类型别名》；cjc 1.0.5 / 1.1.3 零诊断（where 不能写在 typealias 上，见 recovery/whereOnTypeAlias.cj）。
type Id = Int64
type Names = Array<String>
type Matrix = Array<Array<Float64>>
type Handler = (Int64, String) -> Unit
type NamedHandler = (x: Int64, y: String) -> Bool
type Pair = (Int64, Bool)
type Maybe = ?Int64
type Boxes = Array<Array<Int64>>
type AliasOfAlias = Names

func aliasUses(): Unit {
    let a: Id = 1
    let b: Names = ["x"]
    let c: Matrix = [[1.0]]
    let d: Maybe = None
    let e: AliasOfAlias = b
    let f: Handler = { i, s => () }
    let g: NamedHandler = { x: Int64, y: String => true }
    let _ = (a, c, d, e, f, g)
}
