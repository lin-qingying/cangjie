// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface EQ {}

class A<T> {}

interface B<T> {}

extend Int32 <: EQ {}

extend<T> A<T> <: B<T> where T <: EQ {}

func test(a: (EQ) -> Int64) {
    0
}

func subFunc(a: Int32): Int64 {
    0
}

func test2(a: (A<Int32>) -> Int64) {
    0
}

func subFunc2(a: B<Int32>): Int64 {
    0
}

main() {
    test(subFunc)
    test2(subFunc2)
    0
}

/* SCAN
error: no matching function declaration for function reference 'subFunc'
  ==> extend_subtype3.cj:36:10:
   | 
36 |     test(subFunc)
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
