// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output 2>&1 | compare %f

type varr = VArray<Rune, $3>

type varr1_1<T> = VArray<Int64, $3>

// Omit Generic Arguments.

main() {
    var a: varr = varr(repeat: 0)

    var c: varr1_1 = varr1_1(repeat: 0) // error x2
}

/* SCAN
warning: type arg(s) Generics-T are not used
  ==> varray_alias01_err.cj:11:1:
   | 
11 | type varr1_1<T> = VArray<Int64, $3>
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: cannot convert an integer literal to type 'Rune'
  ==> varray_alias01_err.cj:16:32:
   | 
16 |     var a: varr = varr(repeat: 0)
   |                                ^ 
   | 

error: generic type should be used with type argument
  ==> varray_alias01_err.cj:18:12:
   | 
18 |     var c: varr1_1 = varr1_1(repeat: 0) // error x2
   |            ^ 
   | 

2 errors generated, 2 errors printed.
1 warning generated, 1 warning printed.
*/
