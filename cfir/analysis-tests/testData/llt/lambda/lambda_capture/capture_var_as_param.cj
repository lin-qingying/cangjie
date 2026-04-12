// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a = 1
    func f(x: Int64, f: (Int64) -> Int64) {
        return f(x)
    }
    func g(x: Int64) {
        return x ^ 2 + a ^ 2
    }
    var lambda = {b: Int64, g: (Int64) -> Int64 => g(b)}
    f(1, {t => t * a})
    lambda(2, {t => t * a})
    lambda(3, g)
    return 0
}

/* SCAN
error: lambda capturing mutable variables needs to be called directly
  ==> capture_var_as_param.cj:18:10:
   | 
18 |     f(1, {t => t * a})
   |          ^^^^^^^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_param.cj:18:20:
   | 
18 |     f(1, {t => t * a})
   |                    ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture_var_as_param.cj:19:15:
   | 
19 |     lambda(2, {t => t * a})
   |               ^^^^^^^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_param.cj:19:25:
   | 
19 |     lambda(2, {t => t * a})
   |                         ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> capture_var_as_param.cj:20:15:
   | 
20 |     lambda(3, g)
   |               ^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_param.cj:15:24:
   | 
15 |         return x ^ 2 + a ^ 2
   |                        ^ 
   | 

3 errors generated, 3 errors printed.
*/
