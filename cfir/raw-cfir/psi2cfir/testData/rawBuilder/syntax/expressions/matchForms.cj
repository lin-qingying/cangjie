// RUN_PIPELINE_TILL: FRONTEND
// match：带 / 不带匹配值；case 形态：通配符 _、绑定模式 v: T（带 where 守卫）、常量（数字 / 字符串 / Rune / 布尔，数字可用 | 组合）、
// 元组（含嵌套）、enum 构造器（含带参）、Option；match 作为语句、let 初值、返回值、嵌套 match、成员函数内 this 匹配。
// 官方依据：Cangjie 文档《match 表达式》；cjc 1.0.5 / 1.1.3 零诊断（range 模式 `case 1..10` 是官方 parse 错误，见 recovery/matchRangePattern.cj）。
enum Shape {
    | Circle(Int64)
    | Rect(Int64, Int64)
    | Empty
}

enum E2 {
    | A
    | B
}

func matchForms(x: Any, n: Int64, s: String, r: Rune, flag: Bool, t: (Int64, (String, Bool)), o: ?Int64, sh: Shape, e: E2): Unit {
    let byAny = match (x) {
        case v: Int64 => v
        case v: String => 1
        case _ => 0
    }
    let guarded = match (x) {
        case v: Int64 where v > 0 => v
        case _ => 0
    }
    let constants = match (n) {
        case 0 => 0
        case 1 | 2 | 3 => 1
        case _ => 2
    }
    let strMatch = match (s) {
        case "a" => 1
        case "b" => 2
        case _ => 0
    }
    let runeMatch = match (r) {
        case r'a' => 1
        case _ => 0
    }
    let boolMatch = match (flag) {
        case true => 1
        case false => 0
    }
    let tupleMatch = match (t) {
        case (a, (b, c)) => a
    }
    let optionMatch = match (o) {
        case Some(v) => v
        case None => 0
    }
    let shapeMatch = match (sh) {
        case Circle(rad) => rad
        case Rect(w, h) => w * h
        case Empty => 0
    }
    let enumOr = match (e) {
        case E2.A | E2.B => 1
    }
    let noSelector = match {
        case n > 10 => 1
        case n > 1 => 2
        case _ => 3
    }
    let nested = match (n) {
        case 0 => match (s) {
            case "a" => 1
            case _ => 0
        }
        case _ => 0
    }
    match (n) {
        case 1 => println("one")
        case _ => println("other")
    }
    let _ = (byAny, guarded, constants, strMatch, runeMatch, boolMatch, tupleMatch, optionMatch, shapeMatch, enumOr, noSelector, nested)
}

class WithMatch {
    var kind: Int64 = 1

    func describe(): Int64 {
        return match (this.kind) {
            case 0 => 0
            case _ => 1
        }
    }
}
