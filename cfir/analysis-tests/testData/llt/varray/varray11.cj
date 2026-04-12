// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct S {
    let a: VArray<VArray<Int64, $3>, $3> = VArray<VArray<Int64, $3>, $3>(repeat: [0, 1, 2])
    public prop b: VArray<Int64, $3> {
        get() {
            return [1, 2, 3]
        }
    }
}

func f(a: VArray<Int64, $3>) {
    a[1] = 2 // error
}

func f1(a: VArray<VArray<Int64, $3>, $3>) {
    a[1][2] = 2 // error
}

func f3(a: S) {
    a.a[1][2] = 2 //error
}

main() {
    var a = VArray<Int64, $3>(repeat: 0)
    a[1] = 2 // ok
    let b = VArray<Int64, $3>(repeat: 0)
    b[1] = 2 // error

    var arr = VArray<VArray<Int64, $3>, $2>(repeat: [1, 2, 3])
    arr[1][2] = 1 // error

    var varr = Array<VArray<Int64, $3>>(2, repeat: [1, 2, 3])
    varr[1][2] = 1 // error

    var s = S()
    s.b[1] = 1 // error
    0
}

/* SCAN
error: cannot assign to immutable value
  ==> varray11.cj:19:5:
   | 
19 |     a[1] = 2 // error
   |     ^^^^^^^^ 
   | 
note: parameter 'a' is immutable
  ==> varray11.cj:18:8:
   | 
18 | func f(a: VArray<Int64, $3>) {
   |        ^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:23:5:
   | 
23 |     a[1][2] = 2 // error
   |     ^^^^^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:27:5:
   | 
27 |     a.a[1][2] = 2 //error
   |     ^^^^^^^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:34:5:
   | 
34 |     b[1] = 2 // error
   |     ^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> varray11.cj:33:9:
   | 
33 |     let b = VArray<Int64, $3>(repeat: 0)
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:37:5:
   | 
37 |     arr[1][2] = 1 // error
   |     ^^^^^^^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:40:5:
   | 
40 |     varr[1][2] = 1 // error
   |     ^^^^^^^^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray11.cj:43:5:
   | 
43 |     s.b[1] = 1 // error
   |     ^^^^^^^^^^ 
   | 
note: property 'b' is immutable
  ==> varray11.cj:11:17:
   | 
11 |     public prop b: VArray<Int64, $3> {
   |                 ^ 
   | 

7 errors generated, 7 errors printed.
*/
