// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    case60()
    return 0
}

//TEST-CASE:60
//	Step:match (expression_var) { case ERROR_IDENTIFIER_tuplePattern => }
//	Expect:Fail
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
        case (<!TYPE_MISMATCH!>50<!>, 90) => "No"
        case (_, _) => "None"
    }
}

//tuple pattern 中的子 pattern 个数需要和 selector 的维度相同
func case60_3() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case <!TUPLE_PATTERN_NOT_MATCH!>("a", 50, 90)<!> => "No"
        case (_, _) => "None"
    }
}

func case60_4() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case (<!TYPE_MISMATCH!>50<!>, 90) => "No"
        case (_, _) => "None"
    }
}

func case60_5() {
    let a: Float32 = <!TYPE_MISMATCH!>90<!>
    let b: Int32 = 0
    let scoreTuple = ("a", a)
    var result: String = match (scoreTuple) {
        case ("a", <!TYPE_MISMATCH!>0<!>) => "No"
        case (_, _) => "None"
    }
}

func case60_6() {
    let a: Float32 = <!TYPE_MISMATCH!>90<!>
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
