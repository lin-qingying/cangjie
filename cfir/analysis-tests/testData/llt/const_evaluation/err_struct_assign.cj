// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct Foo {
    let a = 0
    var b = 0
    static const c = 0
    const init() {}
}

func f() {
    let v = Foo()
    v.a = 1
    v.b++
    Foo.c = 1
}

main() {}

/* SCAN
error: cannot assign to immutable value
  ==> err_struct_assign.cj:18:5:
   | 
18 |     v.a = 1
   |     ^^^^^^^ 
   | 
note: variable 'a' is immutable
  ==> err_struct_assign.cj:10:9:
   | 
10 |     let a = 0
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> err_struct_assign.cj:19:5:
   | 
19 |     v.b++
   |     ^^^^^ 
   | 
note: variable 'v' is immutable
  ==> err_struct_assign.cj:17:9:
   | 
17 |     let v = Foo()
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> err_struct_assign.cj:20:5:
   | 
20 |     Foo.c = 1
   |     ^^^^^^^^^ 
   | 
note: variable 'c' is immutable
  ==> err_struct_assign.cj:12:18:
   | 
12 |     static const c = 0
   |                  ^ 
   | 

3 errors generated, 3 errors printed.
*/
