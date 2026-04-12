// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A<T> {
    A(_: T) {}
    init(_: Int64) {}
}

main() {
    let _ = A(0)
}

/* SCAN
error: generic instantiation 'A<Int64>' causes ambiguous function 'init'
  ==> err_ambiguous.cj:15:13:
   | 
15 |     let _ = A(0)
   |             ^ 
   | 
note: found candidate
  ==> err_ambiguous.cj:10:5:
   | 
10 |     A(_: T) {}
   |     ^ 
   | 
note: found candidate
  ==> err_ambiguous.cj:11:5:
   | 
11 |     init(_: Int64) {}
   |     ^^^^ 
   | 

1 error generated, 1 error printed.
*/
