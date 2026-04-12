// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A<T> {
    public func test(a: A<T>, b: A<(A<T>, A<T>)>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32> = A<Int32>()
    var b: A<(A<Int32>, A<Int32>)> = A<(A<Int32>, A<Int32>)>()
    A<Int32>().test(a, b)
    return 0
}

/* SCAN
error: generic infinite instantiation
  ==> class10_test2.cj:10:34:
   | 
10 |     public func test(a: A<T>, b: A<(A<T>, A<T>)>): Bool {
   |                                  ^ 
   | 

1 error generated, 1 error printed.
*/
