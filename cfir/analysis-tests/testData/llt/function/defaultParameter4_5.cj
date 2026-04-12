// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
    return
}

main(): Int64 {
    foo0()
    foo0(1)
    foo0(1, 2)
    foo0(1, 2, 3)
    foo0(1, b: 2, c: 3, 4)
    return 0
}

/* SCAN
error: missing arguments for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_5.cj:14:9:
   | 
14 |     foo0()
   |         ^^ expected 3 arguments, found 0
   | 
note: found candidate
 ==> defaultParameter4_5.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
  |      ^^^^ 
  | 

error: missing arguments for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_5.cj:15:9:
   | 
15 |     foo0(1)
   |         ^^^ expected 3 arguments, found 1
   | 
note: found candidate
 ==> defaultParameter4_5.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
  |      ^^^^ 
  | 

error: missing argument prefix 'b:' or 'c:' for named parameter
  ==> defaultParameter4_5.cj:16:13:
   | 
16 |     foo0(1, 2)
   |             ^ 
   | 

error: missing argument prefix 'b:' or 'c:' for named parameter
  ==> defaultParameter4_5.cj:17:13:
   | 
17 |     foo0(1, 2, 3)
   |             ^ 
   | 

error: extra argument given for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_5.cj:18:9:
   | 
18 |     foo0(1, b: 2, c: 3, 4)
   |         ^^^^^^^^^^^^^^^^^^ expected 3 arguments, found 4
   | 
note: found candidate
 ==> defaultParameter4_5.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
  |      ^^^^ 
  | 

5 errors generated, 5 errors printed.
*/
