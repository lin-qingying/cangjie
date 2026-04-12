// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    func f() {
        var x = 1
        func g() {
            x
        }
        g
    }
    {
        x: Int64, y: Int64 =>
        var a = x + y
        {=> 2 * a}
    }
    return 0
}

/* SCAN
error: function capturing mutable variables needs to be called directly
  ==> capture_var_as_return.cj:15:9:
   | 
15 |         g
   |         ^ 
   | 
note: 'x' is mutable
  ==> capture_var_as_return.cj:13:13:
   | 
13 |             x
   |             ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture_var_as_return.cj:20:9:
   | 
20 |         {=> 2 * a}
   |         ^^^^^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_return.cj:20:17:
   | 
20 |         {=> 2 * a}
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
