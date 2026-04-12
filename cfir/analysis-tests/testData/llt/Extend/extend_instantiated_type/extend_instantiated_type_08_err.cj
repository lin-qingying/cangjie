// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

class B<T> {}

class C<T> {}

extend<T> A<C<T>> {
    func test() {
        22
    }
}

main() {
    let a1 = A<B<Int64>>()
    a1.test()
    return 0
}

/* SCAN
error: 'test' is not a member of class 'A<Class-B<Int64>>'
  ==> extend_instantiated_type_08_err.cj:23:8:
   | 
23 |     a1.test()
   |        ^^^^ 
   | 

1 error generated, 1 error printed.
*/
