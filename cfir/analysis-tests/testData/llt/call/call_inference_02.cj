// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A<T> {
    public func test(a: Int64): Unit {}
}

extend<T> A<T> {
    public func test(): Unit {}
}

main(): Int64 {
    var a = A<Int64>()
    a.test<Int64>(1)
    a.test<Rune>()
    return 0
}

/* SCAN
error: non-generic function should not be used with type argument
  ==> call_inference_02.cj:19:5:
   | 
19 |     a.test<Int64>(1)
   |     ^ 
   | 

error: non-generic function should not be used with type argument
  ==> call_inference_02.cj:20:5:
   | 
20 |     a.test<Rune>()
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
