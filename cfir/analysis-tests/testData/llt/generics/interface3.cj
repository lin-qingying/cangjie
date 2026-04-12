// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

interface A<R> {}

interface Test<T, R> {
    static func test(t: R): T<R> {}
    func test1(t: T): A<T>
}

main(): Int64 {
    return 0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> interface3.cj:12:29:
   | 
12 |     static func test(t: R): T<R> {}
   |                             ^^^^ 
   | 

1 error generated, 1 error printed.
*/
