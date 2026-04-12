// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {}

func foo(a: VArray<A, $1>): VArray<A, $1> { // error x2
    return [A()]
}

main() {
    var a: VArray<A, $1> = [A()] // error
    var b = VArray<A, $1>(repeat: A()) // error
    var c = VArray<A, $1> {i => A()} // error
    0
}

/* SCAN
error: 'Class-A' directly or indirectly contains an unsupported type
  ==> varray_with_reftype01.cj:11:36:
   | 
11 | func foo(a: VArray<A, $1>): VArray<A, $1> { // error x2
   |                                    ^ contain unsupported instance member variable with type 'Class-A'
   | 

error: 'Class-A' directly or indirectly contains an unsupported type
  ==> varray_with_reftype01.cj:11:20:
   | 
11 | func foo(a: VArray<A, $1>): VArray<A, $1> { // error x2
   |                    ^ contain unsupported instance member variable with type 'Class-A'
   | 

error: 'Class-A' directly or indirectly contains an unsupported type
  ==> varray_with_reftype01.cj:16:19:
   | 
16 |     var a: VArray<A, $1> = [A()] // error
   |                   ^ contain unsupported instance member variable with type 'Class-A'
   | 

error: 'Class-A' directly or indirectly contains an unsupported type
  ==> varray_with_reftype01.cj:17:20:
   | 
17 |     var b = VArray<A, $1>(repeat: A()) // error
   |                    ^ contain unsupported instance member variable with type 'Class-A'
   | 

error: 'Class-A' directly or indirectly contains an unsupported type
  ==> varray_with_reftype01.cj:18:20:
   | 
18 |     var c = VArray<A, $1> {i => A()} // error
   |                    ^ contain unsupported instance member variable with type 'Class-A'
   | 

5 errors generated, 5 errors printed.
*/
