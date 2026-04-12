// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface EQ {}

class S <: EQ {}

class A<T> {}

extend<T> A<T> where T <: EQ {
    static func add() {}
}

main() {
    A<S>.add()
    A<Int32>.add()
    0
}

/* SCAN
error: 'add' is not a member of class 'A<Int32>'
  ==> extend_generic7.cj:21:14:
   | 
21 |     A<Int32>.add()
   |              ^^^ 
   | 

1 error generated, 1 error printed.
*/
