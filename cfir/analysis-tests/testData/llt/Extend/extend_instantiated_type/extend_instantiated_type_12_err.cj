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

extend<M, N> A<(M, N)> <: I where N <: Int64 {
    func goo() {
        33
    }
}

main() {
    let a = A<(Rune, Rune)>()
    let v1 = a.goo()

    let v2: I = a
    return 0
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-N' must be class or interface
  ==> extend_instantiated_type_12_err.cj:17:11:
   | 
17 | extend<M, N> A<(M, N)> <: I where N <: Int64 {
   |           ^ 
   | 

error: 'goo' is not a member of class 'A<Tuple<Rune, Rune>>'
  ==> extend_instantiated_type_12_err.cj:25:16:
   | 
25 |     let v1 = a.goo()
   |                ^^^ 
   | 

error: mismatched types
  ==> extend_instantiated_type_12_err.cj:27:17:
   | 
27 |     let v2: I = a
   |                 ^ expected 'Interface-I', found 'Class-A<Tuple<Rune, Rune>>'
   | 

3 errors generated, 3 errors printed.
*/
