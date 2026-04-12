// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class GennericClassA<T, V> {}

type TypeClass1<T, V> = GennericClassA<T, V>

type TypeClass2<T> = GennericClassA<T, T>

type TypeClass3<X, Y> = TypeClass2<X>

main() {
    var a1: TypeClass3<Int32>
    var a2 = TypeClass3<Int32>()
    0
}

/* SCAN
warning: type arg(s) Generics-Y are not used
  ==> typealias34.cj:15:1:
   | 
15 | type TypeClass3<X, Y> = TypeClass2<X>
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: type argument's number does not match type parameter's number
  ==> typealias34.cj:18:13:
   | 
18 |     var a1: TypeClass3<Int32>
   |             ^^^^^^^^^^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> typealias34.cj:19:14:
   | 
19 |     var a2 = TypeClass3<Int32>()
   |              ^^^^^^^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
1 warning generated, 1 warning printed.
*/
