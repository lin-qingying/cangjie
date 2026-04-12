// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I1<T> {
    func f(): Array<T> {
        []
    }
    func g() {
        let v = f()
    }
}

class A<N> {}

extend A<Int64> <: I1<Int64> {
    // func foo(a: Int64) {}
}

extend A<String> <: I1<String> {
    // func foo(a: Int64) {}
}

main() {
    return 0
}

/* SCAN
error: extend member 'g' is not allowed to shadow members of 'extend Class-A<Int64>'
  ==> same_sign_default_in_multi_extend_impl02.cj:13:10:
   | 
13 |     func g() {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
