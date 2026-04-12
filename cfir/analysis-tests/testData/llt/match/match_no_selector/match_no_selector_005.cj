// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {}

class B <: A {}

class C <: A {}

main(): Int64 {
    let a = A()
    var b = B()
    var c = C()
    var res: B = match {
        case false => a
        case _ => c
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> match_no_selector_005.cj:20:23:
   | 
20 |         case false => a
   |                       ^ expected 'Class-B', found 'Class-A'
   | 

error: mismatched types
  ==> match_no_selector_005.cj:21:19:
   | 
21 |         case _ => c
   |                   ^ expected 'Class-B', found 'Class-C'
   | 

2 errors generated, 2 errors printed.
*/
