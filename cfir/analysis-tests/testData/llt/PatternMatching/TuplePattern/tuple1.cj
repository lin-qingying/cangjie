


main(): Int64 {
    case60()
    return 0
}

//	Step:match (expression_var) { case ERROR_IDENTIFIER_tuplePattern => }
func case60() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "Yes"
        case ("a", <!TYPE_MISMATCH!>(50, 90)<!>) => "No"
        case (_, _) => "None"
    }
}

func case60_2() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "Yes"
        case (<!CANNOT_CONVERT_LITERAL!>50<!>, 90) => "No"
        case (_, _) => "None"
    }
}

//tuple pattern 中的子 pattern 个数需要和 selector 的维度相同
func case60_3() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>("a", 50, 90)<!> => "No"
        case (_, _) => "None"
    }
}

func case60_4() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case (<!CANNOT_CONVERT_LITERAL!>50<!>, 90) => "No"
        case (_, _) => "None"
    }
}

func case60_5() {
    let a: Float32 = <!CANNOT_CONVERT_LITERAL!>90<!>
    let b: Int32 = 0
    let scoreTuple = ("a", a)
    var result: String = match (scoreTuple) {
        case ("a", <!CANNOT_CONVERT_LITERAL!>0<!>) => "No"
        case (_, _) => "None"
    }
}

func case60_6() {
    let a: Float32 = <!CANNOT_CONVERT_LITERAL!>90<!>
    let scoreTuple = ("a", a)
    var result: String = match (scoreTuple) {
        case ("a", b) => "No"
        case (_, _) => "None"
    }
}

//每个 pattern 之间使用逗号分隔(pattern_1, pattern_2, … pattern_k)
func case60_11() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "Yes"
        case (_, _) => "None"
    }
}

func case60_12() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "No"
        case (_, _) => "None"
    }
}

func case60_13() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "No"
        case (_, _) => "None"
    }
}

func case60_14() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "No"
        case (_, _) => "None"
    }
}
