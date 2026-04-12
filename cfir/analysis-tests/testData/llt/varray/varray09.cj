// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {}

extend Int64 <: I {}

main() {
    let a1_1: Array<Int64> = VArray<Int64, $3>() {i => i} // x2 error
    let a1_2: Array<Int64> = VArray<Int64, $3> {i => i} // x2 error

    let a2_1: VArray<I, $3> = VArray<Int64, $3>() {i => i} // x2 error
    let a2_2: VArray<I, $3> = VArray<Int64, $3> {i => i} // x2 error

    let a3_1: VArray<Any, $3> = VArray<I, $3>() {i => i} // x2 error
    let a3_2: VArray<Any, $3> = VArray<I, $3> {i => i} // x2 error
    0
}

/* SCAN
error: mismatched types
  ==> varray09.cj:14:30:
   | 
14 |     let a1_1: Array<Int64> = VArray<Int64, $3>() {i => i} // x2 error
   |                              ^^^^^^^^^^^^^^^^^^^ expected 'Struct-Array<Int64>', found 'VArray<Int64, $3>'
   | 

error: mismatched types
  ==> varray09.cj:15:30:
   | 
15 |     let a1_2: Array<Int64> = VArray<Int64, $3> {i => i} // x2 error
   |                              ^^^^^^^^^^^^^^^^^ expected 'Struct-Array<Int64>', found 'VArray<Int64, $3>'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray09.cj:17:22:
   | 
17 |     let a2_1: VArray<I, $3> = VArray<Int64, $3>() {i => i} // x2 error
   |                      ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray09.cj:17:31:
   | 
17 |     let a2_1: VArray<I, $3> = VArray<Int64, $3>() {i => i} // x2 error
   |                               ^^^^^^^^^^^^^^^^^^^ expected 'VArray<Interface-I, $3>', found 'VArray<Int64, $3>'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray09.cj:18:22:
   | 
18 |     let a2_2: VArray<I, $3> = VArray<Int64, $3> {i => i} // x2 error
   |                      ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray09.cj:18:31:
   | 
18 |     let a2_2: VArray<I, $3> = VArray<Int64, $3> {i => i} // x2 error
   |                               ^^^^^^^^^^^^^^^^^ expected 'VArray<Interface-I, $3>', found 'VArray<Int64, $3>'
   | 

error: 'Interface-Any' directly or indirectly contains an unsupported type
  ==> varray09.cj:20:22:
   | 
20 |     let a3_1: VArray<Any, $3> = VArray<I, $3>() {i => i} // x2 error
   |                      ^^^ contain unsupported instance member variable with type 'Interface-Any'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray09.cj:20:40:
   | 
20 |     let a3_1: VArray<Any, $3> = VArray<I, $3>() {i => i} // x2 error
   |                                        ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray09.cj:20:33:
   | 
20 |     let a3_1: VArray<Any, $3> = VArray<I, $3>() {i => i} // x2 error
   |                                 ^^^^^^^^^^^^^^^ expected 'VArray<Interface-Any, $3>', found 'VArray<Interface-I, $3>'
   | 

error: 'Interface-Any' directly or indirectly contains an unsupported type
  ==> varray09.cj:21:22:
   | 
21 |     let a3_2: VArray<Any, $3> = VArray<I, $3> {i => i} // x2 error
   |                      ^^^ contain unsupported instance member variable with type 'Interface-Any'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray09.cj:21:40:
   | 
21 |     let a3_2: VArray<Any, $3> = VArray<I, $3> {i => i} // x2 error
   |                                        ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray09.cj:21:33:
   | 
21 |     let a3_2: VArray<Any, $3> = VArray<I, $3> {i => i} // x2 error
   |                                 ^^^^^^^^^^^^^ expected 'VArray<Interface-Any, $3>', found 'VArray<Interface-I, $3>'
   | 

12 errors generated, 12 errors printed.
*/
