// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

var c_f1: CFunc<(CPointer<Int32>) -> Unit> = {
    a =>
}
var c_f2: CFunc<(VArray<Int32, $1>) -> Unit> = {
    a =>
}
var c_f3: CFunc<(CPointer<VArray<Int32, $1>>) -> Unit> = {
    a =>
}
var c_f4: CFunc<(VArray<VArray<Int32, $1>, $1>) -> Unit> = {
    a =>
}

class A {}

main() {
    unsafe {
        var a = VArray<Int32, $1>({i => Int32(i)})
        c_f2(a) // error
        c_f3(inout a) // error

        var a1 = VArray<Int32, $2>({i => Int32(i)})
        c_f2(inout a1) // error

        var b = VArray<VArray<Int32, $1>, $1>(repeat: [1])
        c_f1(inout b) // error
        c_f2(inout b) // error

        var b1 = VArray<VArray<Int32, $2>, $1>(repeat: [1, 2])
        c_f3(inout b1) // error
        c_f4(inout b1) // error

        var b2 = VArray<VArray<Int32, $1>, $2>(repeat: [1])
        c_f4(inout b2) // error
    }
    0
}

/* SCAN
error: mismatch 'inout' of function argument with type 'VArray'
  ==> varray_inout03.cj:27:14:
   | 
27 |         c_f2(a) // error
   |              ^ 
   | 

error: mismatched types
  ==> varray_inout03.cj:28:20:
   | 
28 |         c_f3(inout a) // error
   |                    ^ expected 'VArray<VArray<Int32, $1>, $1>', found 'VArray<Int32, $1>'
   | 

error: mismatched types
  ==> varray_inout03.cj:31:20:
   | 
31 |         c_f2(inout a1) // error
   |                    ^^ expected 'VArray<Int32, $1>', found 'VArray<Int32, $2>'
   | 

error: mismatched types
  ==> varray_inout03.cj:34:20:
   | 
34 |         c_f1(inout b) // error
   |                    ^ expected 'VArray<Int32, $1>', found 'VArray<VArray<Int32, $1>, $1>'
   | 

error: mismatched types
  ==> varray_inout03.cj:35:20:
   | 
35 |         c_f2(inout b) // error
   |                    ^ expected 'VArray<Int32, $1>', found 'VArray<VArray<Int32, $1>, $1>'
   | 

error: mismatched types
  ==> varray_inout03.cj:38:20:
   | 
38 |         c_f3(inout b1) // error
   |                    ^^ expected 'VArray<VArray<Int32, $1>, $1>', found 'VArray<VArray<Int32, $2>, $1>'
   | 

error: mismatched types
  ==> varray_inout03.cj:39:20:
   | 
39 |         c_f4(inout b1) // error
   |                    ^^ expected 'VArray<VArray<Int32, $1>, $1>', found 'VArray<VArray<Int32, $2>, $1>'
   | 

error: mismatched types
  ==> varray_inout03.cj:42:20:
   | 
42 |         c_f4(inout b2) // error
   |                    ^^ expected 'VArray<VArray<Int32, $1>, $1>', found 'VArray<VArray<Int32, $1>, $2>'
   | 

8 errors generated, 8 errors printed.
*/
