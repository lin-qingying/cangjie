// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum Unit2 {
    u2_10(Int64)
    | u2_110(Int64, Int64)
    | u2_1110(Int64, Int64, Int64)
    | u2_1110(Int64, Int32, Int64, Int32)
}

main(): Int64 {
    var u2 = Unit2.u2_1110(10, 100, 1000)
    match (u2) {
        case u2_10(x) | u2_110(x, z) => x
        case u2_10(z) | u2_110(y, _) => 0
        case u2_10(a) | u2_1110(_, a, _, _) => 0
        case _ => 0
    }
    return 0
}

/* SCAN
error: cannot introduce variables in patterns connected by '|'
  ==> enum4.cj:19:20:
   | 
19 |         case u2_10(x) | u2_110(x, z) => x
   |                    ^           ~  ~ 
   | 

error: undeclared identifier 'x'
  ==> enum4.cj:19:41:
   | 
19 |         case u2_10(x) | u2_110(x, z) => x
   |                                         ^ 
   | 

error: cannot introduce variables in patterns connected by '|'
  ==> enum4.cj:20:20:
   | 
20 |         case u2_10(z) | u2_110(y, _) => 0
   |                    ^           ~ 
   | 

error: cannot introduce variables in patterns connected by '|'
  ==> enum4.cj:21:20:
   | 
21 |         case u2_10(a) | u2_1110(_, a, _, _) => 0
   |                    ^               ~ 
   | 

4 errors generated, 4 errors printed.
*/
