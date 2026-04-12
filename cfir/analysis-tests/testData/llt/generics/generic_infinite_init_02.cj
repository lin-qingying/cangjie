// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A<T> {
    func test(a: B<A<T>>) {}
}

class B<T> {
    func test(a: A<(T, T)>) {}
}

main() {
    A<Int32>()
    0
}

/* SCAN
error: generic infinite instantiation
  ==> generic_infinite_init_02.cj:10:18:
   | 
10 |     func test(a: B<A<T>>) {}
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
