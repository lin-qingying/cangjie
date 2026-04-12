// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
func test() {
    var a: A<Int64> = A<Int64>()
    a.test(1)
}

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

struct A<T> <: A<A<T>> & I<T> {
}

main() {
    var a: A<Int64> = A<Int64>()
    a.test(a)
}

/* SCAN
error: struct 'A' can only implement interface
  ==> invalid_struct_inheritance.cj:15:1:
   | 
15 | struct A<T> <: A<A<T>> & I<T> {
   | ^ 
   | 

error: mismatched types
  ==> invalid_struct_inheritance.cj:20:12:
   | 
20 |     a.test(a)
   |            ^ expected 'Int64', found 'Struct-A<Int64>'
   | 

2 errors generated, 2 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
