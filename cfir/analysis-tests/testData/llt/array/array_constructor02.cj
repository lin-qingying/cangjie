// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let arr0 = Array(3, ite: 3)
    let arr1: Array<Rune> = Array(3, repeat: 3)
    let arr2: Array<Rune> = Array<Int64>(3, repeat: 3)
    return 0
}

/* SCAN
error: unknown named argument prefix 'ite:'
  ==> array_constructor02.cj:10:25:
   | 
10 |     let arr0 = Array(3, ite: 3)
   |                         ^ 
   | 

error: unable to infer generic argument of this function
  ==> array_constructor02.cj:11:29:
   | 
11 |     let arr1: Array<Rune> = Array(3, repeat: 3)
   |                             ^^^^^ 
   | 
note: following constraints for type variable 'Generics-T' cannot be solved:
      'Int64 <: Generics-T'
      'Generics-T <: Rune'
  ==> (package std.core)array.cj:12:21:
note: constraint 'Int64 <: Generics-T' may come from:
  ==> array_constructor02.cj:11:38:
   | 
11 |     let arr1: Array<Rune> = Array(3, repeat: 3)
   |                                      ^^^^^^^^^ of type 'Int64', should match parameter type 'Generics-T'
   | 
note: constraint 'Generics-T <: Rune' may come from:
  ==> array_constructor02.cj:11:29:
   | 
11 |     let arr1: Array<Rune> = Array(3, repeat: 3)
   |                             ^^^^^^^^^^^^^^^^^^^ of return type 'Struct-Array<Generics-T>', should match expected type 'Struct-Array<Rune>'
   | 

error: mismatched types
  ==> array_constructor02.cj:12:29:
   | 
12 |     let arr2: Array<Rune> = Array<Int64>(3, repeat: 3)
   |                             ^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Struct-Array<Rune>', found 'Struct-Array<Int64>'
   | 

3 errors generated, 3 errors printed.
*/
