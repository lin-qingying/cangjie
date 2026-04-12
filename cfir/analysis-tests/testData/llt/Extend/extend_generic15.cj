// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A<T> {}

extend<T> A<T> where T <: Rune {
    func foo() {
        123
    }
}

class B <: A<Int64> {}

main() {
    let b = B()
    if (b.foo() != 123) { // cannot access foo
        return 1
    }
    return 0
}

/* SCAN
error: the upper bound 'Rune' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic15.cj:11:8:
   | 
11 | extend<T> A<T> where T <: Rune {
   |        ^ 
   | 

error: 'foo' is not a member of class 'B'
  ==> extend_generic15.cj:21:11:
   | 
21 |     if (b.foo() != 123) { // cannot access foo
   |           ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
