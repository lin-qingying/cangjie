// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

class C1<T> {}

class C2<T> {}

interface A {
    func foo(a: C1<C2<Int32>>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C1<C2<Int32>>): Int32 {
        return 0
    }
}

class D <: A & B {}

main() {
    return 1
}

/* SCAN
error: interface function 'foo' must be implemented in 'D'
  ==> class_no_override_modifier_invalid_5.cj:25:1:
   | 
25 | class D <: A & B {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
