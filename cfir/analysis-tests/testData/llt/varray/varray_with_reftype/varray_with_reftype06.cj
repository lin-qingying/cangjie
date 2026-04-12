// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

func foo1<T>(x: T): Unit {
    var s1: VArray<S<T>, $1> = [S(x)] // error
    var s2: VArray<T, $1> = [x] //error
}

struct S<T> {
    var a: T
    init(a1: T) {
        a = a1
    }
}

main(): Int64 {
    0
}

/* SCAN
error: 'Struct-S<Generics-T>' directly or indirectly contains an unsupported type
  ==> varray_with_reftype06.cj:10:20:
   | 
10 |     var s1: VArray<S<T>, $1> = [S(x)] // error
   |                    ^^^^ contain unsupported instance member variable with type 'Generics-T'
   | 

error: 'Generics-T' directly or indirectly contains an unsupported type
  ==> varray_with_reftype06.cj:11:20:
   | 
11 |     var s2: VArray<T, $1> = [x] //error
   |                    ^ contain unsupported instance member variable with type 'Generics-T'
   | 

2 errors generated, 2 errors printed.
*/
