// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I1<T> {
    func test(a: T): Int32 {
        1
    }
}

interface I2<T> {
    func test(a: T): Int64 {
        1
    }
}

// conflict signature after default implementations been copied
class A<T, K> <: I2<K> & I1<T> {}

main() {
    let x = A<Int32, Int32>()
    0
}

/* SCAN
error: generic instantiation 'A<Int32, Int32>' causes ambiguous function 'test'
  ==> invariant_override_returntype_02.cj:25:13:
   | 
25 |     let x = A<Int32, Int32>()
   |             ^^^^^^^^^^^^^^^ 
   | 
note: found candidate
  ==> invariant_override_returntype_02.cj:10:10:
   | 
10 |     func test(a: T): Int32 {
   |          ^^^^ 
   | 
note: found candidate
  ==> invariant_override_returntype_02.cj:16:10:
   | 
16 |     func test(a: T): Int64 {
   |          ^^^^ 
   | 

1 error generated, 1 error printed.
*/
