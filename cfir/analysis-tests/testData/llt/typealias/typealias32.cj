// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class GennericClassA<T> {}

type TypeClass<Y> = GennericClassA<Y>

type TypeB<X> = TypeClass<X>

type TypeC = TypeClass<Int64>

main() {
    var a = TypeB()
    var b = TypeClass()
    var c = GennericClassA()
    var d = TypeC()
    0
}

/* SCAN
error: unable to infer generic argument of this function
  ==> typealias32.cj:18:13:
   | 
18 |     var a = TypeB()
   |             ^^^^^ 
   | 

error: unable to infer generic argument of this function
  ==> typealias32.cj:19:13:
   | 
19 |     var b = TypeClass()
   |             ^^^^^^^^^ 
   | 

error: unable to infer generic argument of this function
  ==> typealias32.cj:20:13:
   | 
20 |     var c = GennericClassA()
   |             ^^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
