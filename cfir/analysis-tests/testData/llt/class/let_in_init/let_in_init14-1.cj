// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class Test1 {
    public Test1(let a: Int32, var b: Int32) {
        this.a = a
        this.b = b
    }
}

class Test2 {
    public Test2(let a: Int32, var b: Int32) {
        a = a
        b = b
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init14-1.cj:11:9:
   | 
11 |         this.a = a
   |         ^^^^^^^^^^ 
   | 
note: variable 'a' is immutable
  ==> let_in_init14-1.cj:10:22:
   | 
10 |     public Test1(let a: Int32, var b: Int32) {
   |                      ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init14-1.cj:18:9:
   | 
18 |         a = a
   |         ^^^^^ 
   | 
note: parameter 'a' is immutable
  ==> let_in_init14-1.cj:17:22:
   | 
17 |     public Test2(let a: Int32, var b: Int32) {
   |                      ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init14-1.cj:19:9:
   | 
19 |         b = b
   |         ^^^^^ 
   | 
note: parameter 'b' is immutable
  ==> let_in_init14-1.cj:17:36:
   | 
17 |     public Test2(let a: Int32, var b: Int32) {
   |                                    ^ 
   | 

3 errors generated, 3 errors printed.
*/
