// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1): Unit {
    return
}

func foo2(a: Int32, b!: Int32 = a): Unit {
    return
}

main(): Int64 {
    foo0()
    foo0(1)
    foo0(1, 2)
    foo0(1, 2, 3)
    foo0(1, b: 2, c: 3, 4)
    foo1(1)
    foo1(1, 2)
    foo1(1, 2, 3)
    foo2(1)
    foo2(1, 2)
    foo2(1, 2, 3)
    return 0
}

/* SCAN
error: missing arguments for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_3.cj:22:9:
   | 
22 |     foo0()
   |         ^^ expected 3 arguments, found 0
   | 
note: found candidate
 ==> defaultParameter4_3.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
  |      ^^^^ 
  | 

error: missing arguments for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_3.cj:23:9:
   | 
23 |     foo0(1)
   |         ^^^ expected 3 arguments, found 1
   | 
note: found candidate
 ==> defaultParameter4_3.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
  |      ^^^^ 
  | 

error: missing argument prefix 'b:' or 'c:' for named parameter
  ==> defaultParameter4_3.cj:24:13:
   | 
24 |     foo0(1, 2)
   |             ^ 
   | 

error: missing argument prefix 'b:' or 'c:' for named parameter
  ==> defaultParameter4_3.cj:25:13:
   | 
25 |     foo0(1, 2, 3)
   |             ^ 
   | 

error: extra argument given for parameter list '(Int32, Int32, Int32)' in call
  ==> defaultParameter4_3.cj:26:9:
   | 
26 |     foo0(1, b: 2, c: 3, 4)
   |         ^^^^^^^^^^^^^^^^^^ expected 3 arguments, found 4
   | 
note: found candidate
 ==> defaultParameter4_3.cj:9:6:
  | 
9 | func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
  |      ^^^^ 
  | 

error: missing argument prefix 'arg0:' or 'b:' for named parameter
  ==> defaultParameter4_3.cj:27:10:
   | 
27 |     foo1(1)
   |          ^ 
   | 

error: missing argument prefix 'arg0:' or 'b:' for named parameter
  ==> defaultParameter4_3.cj:28:10:
   | 
28 |     foo1(1, 2)
   |          ^ 
   | 

error: extra argument given for parameter list '(Int32, Int32)' in call
  ==> defaultParameter4_3.cj:29:9:
   | 
29 |     foo1(1, 2, 3)
   |         ^^^^^^^^^ expected 2 arguments, found 3
   | 
note: found candidate
  ==> defaultParameter4_3.cj:13:6:
   | 
13 | func foo1(arg0!: Int32, b!: Int32 = arg0 + 1): Unit {
   |      ^^^^ 
   | 

error: missing argument prefix 'b:' for named parameter
  ==> defaultParameter4_3.cj:31:13:
   | 
31 |     foo2(1, 2)
   |             ^ 
   | 

error: extra argument given for parameter list '(Int32, Int32)' in call
  ==> defaultParameter4_3.cj:32:9:
   | 
32 |     foo2(1, 2, 3)
   |         ^^^^^^^^^ expected 2 arguments, found 3
   | 
note: found candidate
  ==> defaultParameter4_3.cj:17:6:
   | 
17 | func foo2(a: Int32, b!: Int32 = a): Unit {
   |      ^^^^ 
   | 

10 errors generated, 10 errors printed.
*/
