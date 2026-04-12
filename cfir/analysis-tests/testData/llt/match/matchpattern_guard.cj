// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    print(case01(1).toString())
    case02(1)
    return 0
}

func case01(x: Float32) {
    let a: Float32 = 1
    let b: Float32 = 0
    var result: Int64 = match (Year(x)) {
        case _ where false => 1
    }
    return result
}

func case02(x: Float32) {
    let a: Float32 = 1
    let b: Float32 = 0
    var result: Int64 = match (Year(x)) {
        case _ where x != 1 => 1
    }
    return result
}

enum TimeUnit {
    Year(Float32)
}

/* SCAN
error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:16:22:
   | 
16 |     let a: Float32 = 1
   |                      ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:17:22:
   | 
17 |     let b: Float32 = 0
   |                      ^ 
   | 

error: non-exhaustive patterns
  ==> matchpattern_guard.cj:18:32:
   | 
18 |     var result: Int64 = match (Year(x)) {
   |                                ^^^^^^^ the selector is of type 'Enum-TimeUnit'
   | 
   # note: _ is not covered

error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:10:18:
   | 
10 |     print(case01(1).toString())
   |                  ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:25:22:
   | 
25 |     let a: Float32 = 1
   |                      ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:26:22:
   | 
26 |     let b: Float32 = 0
   |                      ^ 
   | 

error: invalid binary operator '!=' on type 'Float32' and 'Int64'
  ==> matchpattern_guard.cj:28:24:
   | 
28 |         case _ where x != 1 => 1
   |                        ^^ 
   | 
   # note: you may want to implement 'operator func !=(right: Int64)' for type 'Float32'

error: cannot convert an integer literal to type 'Float32'
  ==> matchpattern_guard.cj:11:12:
   | 
11 |     case02(1)
   |            ^ 
   | 

8 errors generated, 8 errors printed.
*/
