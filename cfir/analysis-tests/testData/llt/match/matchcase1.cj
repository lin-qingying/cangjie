// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = match (m) {
        case 1 => 1
        case 2 => 2.1
        case _ => 3
    }
    return b
}

/* SCAN
error: cannot convert a floating-point literal to type 'Int32'
  ==> matchcase1.cj:14:19:
   | 
14 |         case 2 => 2.1
   |                   ^ 
   | 

1 error generated, 1 error printed.
*/
