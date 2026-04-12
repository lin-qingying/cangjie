// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

type A<T, k> = structA<T, k>

type TI<T, K> = I<T, O>

var d = r'#'

extend<T, K> structA<T, K> <: TI<T, K> where T <: Rune, K <: String {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

interface I<T, K> {
    mut static prop i: Rune {
        get() {
            d
        }
        set(v) {
            d = v
        }
    }
}

struct structA<T, K> where T <: Rune, K <: String {
    var a: T
    var b: K
    structA(x: T, y: K) {
        this.a = x
        this.b = y
    }
}

main(): Int64 {
    func a(a: A<Rune, String>) {
        return a.a.toString() + a.b
    }

    A<Rune, String>.i = r'@'

    if (a(A<Rune, String>(A<Rune, String>.i, "345")) == "@345") {
        return 0
    } else {
        return 1
    }
}

/* SCAN
error: undeclared type name 'O'
  ==> undeclared_type.cj:11:22:
   | 
11 | type TI<T, K> = I<T, O>
   |                      ^ 
   | 

error: 'I' is not a type
  ==> undeclared_type.cj:11:17:
   | 
11 | type TI<T, K> = I<T, O>
   |                 ^ 
   | 

error: expected an interface, found non-interface type
  ==> undeclared_type.cj:15:31:
   | 
15 | extend<T, K> structA<T, K> <: TI<T, K> where T <: Rune, K <: String {
   |                               ^^^^^^^^ expected an interface here
   | 

error: the upper bound 'Rune' of generic parameter 'Generics-T' must be class or interface
  ==> undeclared_type.cj:15:8:
   | 
15 | extend<T, K> structA<T, K> <: TI<T, K> where T <: Rune, K <: String {
   |        ^ 
   | 

error: the upper bound 'Struct-String' of generic parameter 'Generics-K' must be class or interface
  ==> undeclared_type.cj:15:11:
   | 
15 | extend<T, K> structA<T, K> <: TI<T, K> where T <: Rune, K <: String {
   |           ^ 
   | 

error: the upper bound 'Rune' of generic parameter 'Generics-T' must be class or interface
  ==> undeclared_type.cj:37:16:
   | 
37 | struct structA<T, K> where T <: Rune, K <: String {
   |                ^ 
   | 

error: the upper bound 'Struct-String' of generic parameter 'Generics-K' must be class or interface
  ==> undeclared_type.cj:37:19:
   | 
37 | struct structA<T, K> where T <: Rune, K <: String {
   |                   ^ 
   | 

7 errors generated, 7 errors printed.
*/
