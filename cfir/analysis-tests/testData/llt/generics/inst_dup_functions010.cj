// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %f -o %output 2>&1 | compare %f

class A<T, R> {
    public static func foo(a: Option<T>, b: R) {}
    private static func foo(a: Option<T>, b: Bool) {}
}

main() {
    A<Int64, Bool>.foo(1, true)
}

/* SCAN
error: generic instantiation 'A<Int64, Bool>' causes ambiguous function 'foo'
  ==> inst_dup_functions010.cj:15:5:
   | 
15 |     A<Int64, Bool>.foo(1, true)
   |     ^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions010.cj:10:24:
   | 
10 |     public static func foo(a: Option<T>, b: R) {}
   |                        ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions010.cj:11:25:
   | 
11 |     private static func foo(a: Option<T>, b: Bool) {}
   |                         ^^^ 
   | 

1 error generated, 1 error printed.
*/
