// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo0(a: Int32, b!: Int32 = c + 1, c!: Int32 = a * b) {
    c
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1) {
    b
}

func foo2(a: Int32, b!: Int32 = b) {
    c
}

func run(): Int64 {
    foo1(1)
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: undeclared identifier 'c'
 ==> defaultParameter4_1.cj:9:33:
  | 
9 | func foo0(a: Int32, b!: Int32 = c + 1, c!: Int32 = a * b) {
  |                                 ^ 
  | 

error: undeclared identifier 'b'
  ==> defaultParameter4_1.cj:17:33:
   | 
17 | func foo2(a: Int32, b!: Int32 = b) {
   |                                 ^ 
   | 

error: undeclared identifier 'c'
  ==> defaultParameter4_1.cj:18:5:
   | 
18 |     c
   |     ^ 
   | 

error: missing argument prefix 'arg0:' or 'b:' for named parameter
  ==> defaultParameter4_1.cj:22:10:
   | 
22 |     foo1(1)
   |          ^ 
   | 

4 errors generated, 4 errors printed.
*/
