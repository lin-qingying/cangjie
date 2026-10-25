// match 表达式：常量模式、类型模式、绑定模式、枚举模式与通配符。
// 官方依据：Cangjie 文档《match 表达式》；cjc 1.0.5 / 1.1.3 零诊断。
func describe(value: Int64): String {
    return match (value) {
        case 0 => "zero"
        case 1 => "one"
        case -1 => "minus one"
        case _ => "other"
    }
}

func classify(value: Any): String {
    return match (value) {
        case i: Int64 => "int"
        case s: String => "string"
        case _ => "unknown"
    }
}

func unwrap(option: Option<Int64>): Int64 {
    return match (option) {
        case Some(x) => x
        case None => 0
    }
}

func nested(subject: Int64): String {
    return match (subject) {
        case 1 => match (subject + 1) {
            case 2 => "one-two"
            case _ => "one-other"
        }
        case _ => "other"
    }
}
