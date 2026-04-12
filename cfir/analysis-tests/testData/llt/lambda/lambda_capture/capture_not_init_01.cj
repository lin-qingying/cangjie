// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let x: Int64
    let f = {=> x}
    var y: Int32
    {a: Int32, b: Int32 => a + b + y}
    return 0
}

/* SCAN
error: cannot capture variable 'x' before initialization
  ==> capture_not_init_01.cj:11:17:
   | 
11 |     let f = {=> x}
   |                 ^ 
   | 

error: cannot capture variable 'y' before initialization
  ==> capture_not_init_01.cj:13:36:
   | 
13 |     {a: Int32, b: Int32 => a + b + y}
   |                                    ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture_not_init_01.cj:13:5:
   | 
13 |     {a: Int32, b: Int32 => a + b + y}
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: 'y' is mutable
  ==> capture_not_init_01.cj:13:36:
   | 
13 |     {a: Int32, b: Int32 => a + b + y}
   |                                    ^ 
   | 

3 errors generated, 3 errors printed.
*/
