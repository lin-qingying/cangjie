// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

interface A<T> {
    func foo(a: T): T {
        return a
    }
}

interface B<T> {
    func foo(a: T): T {
        return a
    }
}

class D<T> <: A<T> & B<T> {}

main() {
    return 1
}

/* SCAN
error: interface function 'foo' must be implemented in 'D'
  ==> class_no_override_modifier_invalid_8.cj:21:1:
   | 
21 | class D<T> <: A<T> & B<T> {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
