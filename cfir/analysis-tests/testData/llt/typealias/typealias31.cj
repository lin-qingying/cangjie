// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

type A = TypeClass1<Int64, Int8>

type B<X> = TypeClass2<X>

main() {
    var a1: A<Int32>
    var a2: A = A<Int32>()
    var b1: B<Int64, Int32>
    var b2 = B<Int64, Int32>()
    0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> typealias31.cj:20:13:
   | 
20 |     var a1: A<Int32>
   |             ^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> typealias31.cj:21:17:
   | 
21 |     var a2: A = A<Int32>()
   |                 ^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> typealias31.cj:22:13:
   | 
22 |     var b1: B<Int64, Int32>
   |             ^^^^^^^^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> typealias31.cj:23:14:
   | 
23 |     var b2 = B<Int64, Int32>()
   |              ^^^^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/