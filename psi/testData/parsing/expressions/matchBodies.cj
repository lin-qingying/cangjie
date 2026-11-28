// match 分支体：单表达式、一行多分支、块体与嵌套 match。
// 官方依据：Cangjie 文档《match 表达式》；cjc 1.0.5 / 1.1.3 零诊断。
enum MatchBodyKind {
    | Round(Int64)
    | Flat
}

func matchBodySingleExpr(n: Int64): Int64 {
    return match (n) {
        case 1 => 0
        case _ => 0
    }
}

func matchBodyOneLine(n: Int64): Int64 {
    return match (n) { case 1 => 1; case _ => 0 }
}

func matchBodyBlock(kind: MatchBodyKind): Int64 {
    return match (kind) {
        case Round(n) =>
            let doubled = n * 2
            doubled + 1
        case Flat =>
            0
    }
}

func matchBodyNested(n: Int64): Int64 {
    return match (n) {
        case 1 =>
            match (n + 1) {
                case 2 => 20
                case _ => 21
            }
        case _ => 0
    }
}
