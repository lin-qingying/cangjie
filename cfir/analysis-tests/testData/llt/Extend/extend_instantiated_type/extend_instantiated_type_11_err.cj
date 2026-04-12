// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

interface I {
    func i1() {
        44
    }
}

extend<M, N> A<(M, N)> <: I {
    func goo() {
        33
    }
}

main() {
    let a = A<(Rune, Int64, Rune)>()
    let v1 = a.goo()

    let v2: I = a
    return 0
}

/* SCAN
error: 'goo' is not a member of class 'A<Tuple<Rune, Int64, Rune>>'
  ==> extend_instantiated_type_11_err.cj:25:16:
   | 
25 |     let v1 = a.goo()
   |                ^^^ 
   | 

error: mismatched types
  ==> extend_instantiated_type_11_err.cj:27:17:
   | 
27 |     let v2: I = a
   |                 ^ expected 'Interface-I', found 'Class-A<Tuple<Rune, Int64, Rune>>'
   | 

2 errors generated, 2 errors printed.
*/
