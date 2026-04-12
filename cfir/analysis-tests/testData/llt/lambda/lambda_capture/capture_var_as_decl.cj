// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a = 1
    func f(): Int64 {
        return a
    }
    var lambda = {y: Int64 => y + a}
    var g = f
    return 0
}

/* SCAN
error: lambda capturing mutable variables needs to be called directly
  ==> capture_var_as_decl.cj:14:18:
   | 
14 |     var lambda = {y: Int64 => y + a}
   |                  ^^^^^^^^^^^^^^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_decl.cj:14:35:
   | 
14 |     var lambda = {y: Int64 => y + a}
   |                                   ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> capture_var_as_decl.cj:15:13:
   | 
15 |     var g = f
   |             ^ 
   | 
note: 'a' is mutable
  ==> capture_var_as_decl.cj:12:16:
   | 
12 |         return a
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
