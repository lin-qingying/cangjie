// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %f -o %output 2>&1 | compare %f

interface I {
    func foo(a: Int32): Unit {}
}

extend<T> CPointer<T> <: I {
    func foo(a: T): Int32 {
        1
    }
}

main() {
    let x: CType = CPointer<Int32>()
}

/* SCAN
error: generic instantiation 'CPointer<Int32>' causes ambiguous function 'foo'
  ==> inst_dup_functions009.cj:20:20:
   | 
20 |     let x: CType = CPointer<Int32>()
   |                    ^^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> inst_dup_functions009.cj:10:10:
   | 
10 |     func foo(a: Int32): Unit {}
   |          ^^^ 
   | 
note: found candidate
  ==> inst_dup_functions009.cj:14:10:
   | 
14 |     func foo(a: T): Int32 {
   |          ^^^ 
   | 

1 error generated, 1 error printed.
*/
