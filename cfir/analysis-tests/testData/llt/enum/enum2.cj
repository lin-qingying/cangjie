// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit {
    Year | Month | Day | Hour
}

func codegenUnitTest(): Int32 {
    var time1 = TimeUnit.Year
    time1.Year
    time1.Year()
    return 0
}

/* SCAN
error: no matching function for operator '()' function call
  ==> enum2.cj:16:5:
   | 
16 |     time1.Year()
   |     ^ 
   | 

error: base of member access can not be enum variable
  ==> enum2.cj:15:5:
   | 
15 |     time1.Year
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
