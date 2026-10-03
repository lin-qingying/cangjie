// RUN_PIPELINE_TILL: FRONTEND
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
