// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct B<T> {}

class A<T> {
    public operator func [](b: A<B<T>>): Bool {
        true
    }
    public func test(a: A<T>, b: A<B<T>>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<B<Int32>> = A<B<Int32>>()
    var b: A<Int32> = A<Int32>()
    b[a]
    A<Int32>().test(b, a)
    1
}

/* SCAN
error: generic infinite instantiation
  ==> class11_test1.cj:12:32:
   | 
12 |     public operator func [](b: A<B<T>>): Bool {
   |                                ^ 
   | 

1 error generated, 1 error printed.
*/
