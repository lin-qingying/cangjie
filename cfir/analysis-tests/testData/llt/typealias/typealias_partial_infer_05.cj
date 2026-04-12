// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%compiler %cmp_opt %f  2>&1 | compare %f

// Test for compilation of instantiation
open class I1<F, K> {
    static var a: Int64 = 1
}

extend<F, K> I1<F, K> {}

class I2<K> <: I1<Int64, K> {}

type Y0<K> = I1<Int32, Int8>

type Y1<K> = I1<Int32, K>

type X0<K> = I2<K>

type X1<K> = I2<Int64>

main() {
    Y0.a
    let y0: Int64 = Y1.a
    let y1 = Y0<Int8>.a
    let x0 = X0.a
    let x1 = X0<Int8>.a
    return 0
}

/* SCAN
warning: type arg(s) Generics-K are not used
  ==> typealias_partial_infer_05.cj:18:1:
   | 
18 | type Y0<K> = I1<Int32, Int8>
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: type arg(s) Generics-K are not used
  ==> typealias_partial_infer_05.cj:24:1:
   | 
24 | type X1<K> = I2<Int64>
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: generic type should be used with type argument
  ==> typealias_partial_infer_05.cj:28:21:
   | 
28 |     let y0: Int64 = Y1.a
   |                     ^ 
   | 

error: generic type should be used with type argument
  ==> typealias_partial_infer_05.cj:30:14:
   | 
30 |     let x0 = X0.a
   |              ^ 
   | 

2 errors generated, 2 errors printed.
2 warnings generated, 2 warnings printed.
*/
