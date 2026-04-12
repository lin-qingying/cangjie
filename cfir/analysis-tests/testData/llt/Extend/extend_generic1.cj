// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add() {}
}

main() {
    let a: A<S> = A<S>()
    a.add()
    let b: A<Int32> = A<Int32>()
    b.add()
    0
}

/* SCAN
error: 'add' is not a member of class 'A<Int32>'
  ==> extend_generic1.cj:23:7:
   | 
23 |     b.add()
   |       ^^^ 
   | 

1 error generated, 1 error printed.
*/
