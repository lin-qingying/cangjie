// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit {
    Year(Int32) | Month(Int32)
}

enum TimeUnit1<T> {
    Year(T)
}

func codegenUnitTest(): Int32 {
    var time1 = Year(1)
    var time2 = TimeUnit.Month(r'a')
    return 0
}

/* SCAN
error: find multiple constructor 'Year' of enum declaration
  ==> enum10.cj:18:17:
   | 
18 |     var time1 = Year(1)
   |                 ^ 
   | 
note: found candidate
  ==> enum10.cj:10:5:
   | 
10 |     Year(Int32) | Month(Int32)
   |     ^ 
   | 
note: found candidate
  ==> enum10.cj:14:5:
   | 
14 |     Year(T)
   |     ^ 
   | 

error: cannot convert a character literal to type 'Int32'
  ==> enum10.cj:19:32:
   | 
19 |     var time2 = TimeUnit.Month(r'a')
   |                                ^ 
   | 

2 errors generated, 2 errors printed.
*/
