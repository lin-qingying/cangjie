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
        case ("a", (50, 90)) => "No"
        case (_, _) => "None"
    }
}

func case60_2() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 90) => "Yes"
        case (50, 90) => "No"
        case (_, _) => "None"
    }
}

//tuple pattern 中的子 pattern 个数需要和 selector 的维度相同
func case60_3() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case ("a", 50, 90) => "No"
        case (_, _) => "None"
    }
}

func case60_4() {
    let scoreTuple = ("a", 90)
    var result: String = match (scoreTuple) {
        case (50, 90) => "No"
        case (_, _) => "None"
    }
}

func case60_5() {
    let a: Float32 = 90
    let b: Int32 = 0
    let scoreTuple = ("a", a)
    var result: String = match (scoreTuple) {
        case ("a", 0) => "No"
        case (_, _) => "None"
    }
}

func case60_6() {
    let a: Float32 = 90
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

/* SCAN
error: mismatched types
  ==> tuple1.cj:21:20:
   | 
21 |         case ("a", (50, 90)) => "No"
   |                    ^^^^^^^^ expected 'Int64', found 'Tuple'
   | 

error: cannot convert an integer literal to type 'Struct-String'
  ==> tuple1.cj:30:15:
   | 
30 |         case (50, 90) => "No"
   |               ^ 
   | 

error: tuple pattern with correct size expected
  ==> tuple1.cj:39:14:
   | 
39 |         case ("a", 50, 90) => "No"
   |              ^ 
   | 

error: cannot convert an integer literal to type 'Struct-String'
  ==> tuple1.cj:47:15:
   | 
47 |         case (50, 90) => "No"
   |               ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> tuple1.cj:53:22:
   | 
53 |     let a: Float32 = 90
   |                      ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> tuple1.cj:57:20:
   | 
57 |         case ("a", 0) => "No"
   |                    ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> tuple1.cj:63:22:
   | 
63 |     let a: Float32 = 90
   |                      ^ 
   | 

7 errors generated, 7 errors printed.
*/
