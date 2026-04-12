// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1<T> {
    func foo(a: T): Int64 {
        1
    }
}

interface I2<T> {
    func foo(a: T): Int64 {
        2
    }
}

class C1<T> {}

extend<T> C1<T> <: I1<T> {}

extend<T> C1<T> <: I2<T> {}

main() {
    return -1
}

/* SCAN
error: interface function 'foo' must be implemented in 'extend C1'
  ==> extend_function_conflict_invalid_3.cj:23:1:
   | 
23 | extend<T> C1<T> <: I1<T> {}
   | ^ 
   | 

error: interface function 'foo' must be implemented in 'extend C1'
  ==> extend_function_conflict_invalid_3.cj:25:1:
   | 
25 | extend<T> C1<T> <: I2<T> {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
