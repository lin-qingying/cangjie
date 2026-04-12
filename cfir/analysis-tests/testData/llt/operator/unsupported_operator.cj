// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() : Int64 {
    var a = 1**2
    var b = 1+&1
    var c = 1-&1
    var d = 1*&1
    var e = 1/1
    var f = 1**&1
}

/* SCAN
error: unknown start of token: 1
  ==> unsupported_operator.cj:11:14:
   | 
11 |     var b = 1+&1
   |              ^ unknown start of token: 1
   | 

error: unknown start of token: 1
  ==> unsupported_operator.cj:12:14:
   | 
12 |     var c = 1-&1
   |              ^ unknown start of token: 1
   | 

error: unknown start of token: 1
  ==> unsupported_operator.cj:13:14:
   | 
13 |     var d = 1*&1
   |              ^ unknown start of token: 1
   | 

error: unknown start of token: 1
  ==> unsupported_operator.cj:15:14:
   | 
15 |     var f = 1**&1
   |              ^ unknown start of token: 1
   | 

4 errors generated, 4 errors printed.
*/
