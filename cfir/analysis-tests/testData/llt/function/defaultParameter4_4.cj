// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

main(): Int64 {
    foo0(a: 1)
    foo0(1, d: 2)
    foo0(1, d: 2, e: 3)
    foo0(1, d: 2, e: 3, f: 4)

    return 0
}

/* SCAN
error: missing arguments for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_4.cj:14:9:
   | 
14 |     foo0(a: 1)
   |         ^^^^^^ expected 3 arguments, found 1
   | 
note: found candidate
 ==> defaultParameter4_4.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
  |      ^^^^ 
  | 

error: unknown named argument prefix 'd:'
  ==> defaultParameter4_4.cj:15:13:
   | 
15 |     foo0(1, d: 2)
   |             ^ 
   | 

error: unknown named argument prefix 'd:'
  ==> defaultParameter4_4.cj:16:13:
   | 
16 |     foo0(1, d: 2, e: 3)
   |             ^ 
   | 

error: extra argument given for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_4.cj:17:9:
   | 
17 |     foo0(1, d: 2, e: 3, f: 4)
   |         ^^^^^^^^^^^^^^^^^^^^^ expected 3 arguments, found 4
   | 
note: found candidate
 ==> defaultParameter4_4.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
  |      ^^^^ 
  | 

4 errors generated, 4 errors printed.
*/
