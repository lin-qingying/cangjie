// 常量模式：整型、字符串、布尔字面量作为 match 主语与嵌套 match。
// 官方依据：Cangjie 文档《match 表达式》；cjc 1.0.5 / 1.1.3 零诊断。
func useConst(x: Int64, s: String, b: Bool): Int64 {
    return match (x) {
        case 1 => 1
        case 2 => 2
        case _ => match (s) {
            case "a" => 3
            case _ => match (b) {
                case true => 4
                case _ => 0
            }
        }
    }
}
