// match 高级模式：or 模式、where 守卫、无主语 match、元组与嵌套模式。
// 官方依据：Cangjie 文档《match 表达式》；cjc 1.0.5 / 1.1.3 零诊断。
enum TimeUnit {
    | Year(Int32)
    | Month(Int32, Int32)
}

enum Option<T> {
    | Some(T)
    | None
}

func classifyScore(score: Int64): String {
    return match {
        case score < 60 => "fail"
        case score < 70 => "pass"
        case score < 90 => "good"
        case _ => "excellent"
    }
}

func extractYear(time: TimeUnit): Int32 {
    return match (time) {
        case Year(y) where 1 == 1 => y
        case Year(_) | Month(_, _) => 0
        case Month(y, _) => y
    }
}

func nested(option: Option<(Int64, String)>): Int64 {
    return match (option) {
        case Some((n, _)) => n
        case None => 0
    }
}

func tuplePattern(pair: (Int64, Int64)): Int64 {
    return match (pair) {
        case (0, y) => y
        case (x, 0) => x
        case (x, y) => x + y
    }
}
