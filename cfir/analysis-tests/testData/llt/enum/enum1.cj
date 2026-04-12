// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit {
    Year | Month | Day | Hour
}

enum TimeUnit2 {
    Year2(Int32)
    | Month2(Int32, Int32)
}

func codegenUnitTest(): TimeUnit {
    var time1 = TimeUnit.Year3(1)
    var time2 = TimeUnit2.Month2(1)
    var time3 = TimeUnit2.Year
    var time4 = TimeUnit.Year(1)
    return 0
}

/* SCAN
error: 'Year3' is not a member of enum 'TimeUnit'
  ==> enum1.cj:19:26:
   | 
19 |     var time1 = TimeUnit.Year3(1)
   |                          ^^^^^ 
   | 

error: missing argument for parameter list '(Int32, Int32)' in call
  ==> enum1.cj:20:33:
   | 
20 |     var time2 = TimeUnit2.Month2(1)
   |                                 ^^^ expected 2 arguments, found 1
   | 
note: found candidate
  ==> enum1.cj:15:7:
   | 
15 |     | Month2(Int32, Int32)
   |       ^^^^^^ 
   | 

error: 'Year' is not a member of enum 'TimeUnit2'
  ==> enum1.cj:21:27:
   | 
21 |     var time3 = TimeUnit2.Year
   |                           ^^^^ 
   | 

error: no matching function for operator '()' function call
  ==> enum1.cj:22:17:
   | 
22 |     var time4 = TimeUnit.Year(1)
   |                 ^ 
   | 

error: cannot convert an integer literal to type 'Enum-TimeUnit'
  ==> enum1.cj:23:12:
   | 
23 |     return 0
   |            ^ 
   | 

5 errors generated, 5 errors printed.
*/
