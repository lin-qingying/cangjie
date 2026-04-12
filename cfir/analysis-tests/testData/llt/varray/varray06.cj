// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {}

extend Int64 <: I {}

main() {
    let a: VArray<Int64, $3> = Array<Int64>(3, repeat: 0) // x2 error
    let a1: Array<Int64> = VArray<Int64, $3>(repeat: 0) // x2 error

    let a2: VArray<I, $3> = VArray<Int64, $3>(repeat: 0) // x2 error
    let a3: VArray<Any, $3> = VArray<I, $3>(repeat: 0) // x2 error

    let a7: VArray<Int64, $3> = VArray<Int64, $3>(itxx: 0) // error
    0
}

/* SCAN
error: mismatched types
  ==> varray06.cj:14:32:
   | 
14 |     let a: VArray<Int64, $3> = Array<Int64>(3, repeat: 0) // x2 error
   |                                ^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'VArray<Int64, $3>', found 'Struct-Array<Int64>'
   | 

error: mismatched types
  ==> varray06.cj:15:28:
   | 
15 |     let a1: Array<Int64> = VArray<Int64, $3>(repeat: 0) // x2 error
   |                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Struct-Array<Int64>', found 'VArray<Int64, $3>'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray06.cj:17:20:
   | 
17 |     let a2: VArray<I, $3> = VArray<Int64, $3>(repeat: 0) // x2 error
   |                    ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray06.cj:17:29:
   | 
17 |     let a2: VArray<I, $3> = VArray<Int64, $3>(repeat: 0) // x2 error
   |                             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'VArray<Interface-I, $3>', found 'VArray<Int64, $3>'
   | 

error: 'Interface-Any' directly or indirectly contains an unsupported type
  ==> varray06.cj:18:20:
   | 
18 |     let a3: VArray<Any, $3> = VArray<I, $3>(repeat: 0) // x2 error
   |                    ^^^ contain unsupported instance member variable with type 'Interface-Any'
   | 

error: 'Interface-I' directly or indirectly contains an unsupported type
  ==> varray06.cj:18:38:
   | 
18 |     let a3: VArray<Any, $3> = VArray<I, $3>(repeat: 0) // x2 error
   |                                      ^ contain unsupported instance member variable with type 'Interface-I'
   | 

error: mismatched types
  ==> varray06.cj:18:31:
   | 
18 |     let a3: VArray<Any, $3> = VArray<I, $3>(repeat: 0) // x2 error
   |                               ^^^^^^^^^^^^^^^^^^^^^^^^ expected 'VArray<Interface-Any, $3>', found 'VArray<Interface-I, $3>'
   | 

error: unknown named argument prefix 'itxx:'
  ==> varray06.cj:20:51:
   | 
20 |     let a7: VArray<Int64, $3> = VArray<Int64, $3>(itxx: 0) // error
   |                                                   ^ 
   | 
   # note: expect the name of the named parameter is 'item'

8 errors generated, 8 errors printed.
*/
