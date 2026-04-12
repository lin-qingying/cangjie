// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum Foo {
    A | B | C

    public func f1(v: Foo) {
        v = B
        A = B
    }
}

main(): Int64 {
    0
}

/* SCAN
error: cannot assign to immutable value
  ==> enum30_1.cj:13:9:
   | 
13 |         v = B
   |         ^^^^^ 
   | 
note: parameter 'v' is immutable
  ==> enum30_1.cj:12:20:
   | 
12 |     public func f1(v: Foo) {
   |                    ^ 
   | 

error: cannot assign to immutable value
  ==> enum30_1.cj:14:9:
   | 
14 |         A = B
   |         ^^^^^ 
   | 
note: variable 'A' is immutable
  ==> enum30_1.cj:10:5:
   | 
10 |     A | B | C
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
