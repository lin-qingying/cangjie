// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

func test<X, Y>() where X <: Y {}

class C<X, Y> where X <: Y {}

struct R<X, Y> where X <: Y {}

main() {
    var i = test<Int8, Int8>()
    var c = C<Int8, Int8>()
    var r = R<Int8, Int8>()
    return 0
}

/* SCAN
error: the upper bound 'Generics-Y' of generic parameter 'Generics-X' must be class or interface
 ==> call17.cj:9:11:
  | 
9 | func test<X, Y>() where X <: Y {}
  |           ^ 
  | 

error: the upper bound 'Generics-Y' of generic parameter 'Generics-X' must be class or interface
  ==> call17.cj:11:9:
   | 
11 | class C<X, Y> where X <: Y {}
   |         ^ 
   | 

error: the upper bound 'Generics-Y' of generic parameter 'Generics-X' must be class or interface
  ==> call17.cj:13:10:
   | 
13 | struct R<X, Y> where X <: Y {}
   |          ^ 
   | 

3 errors generated, 3 errors printed.
*/
