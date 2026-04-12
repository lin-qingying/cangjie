// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %cmp_opt_chir2hlir %f -o %n.%suffix 2>&1 | compare %f

interface I2 {}

extend Int32 <: I2 {}

extend Unit <: I2 {}

extend<T> CPointer<T> <: I2 where T <: I2 {}

var node19: I2 = CPointer()
var node20: I2 = CPointer<Int64>()
var node21: I2 = CPointer(CPointer<Int64>())

main() {}

/* SCAN
error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse2.cj:17:18:
   | 
17 | var node19: I2 = CPointer()
   |                  ^ 
   | 

error: mismatched types
  ==> cpointer_generic_reverse2.cj:18:18:
   | 
18 | var node20: I2 = CPointer<Int64>()
   |                  ^^^^^^^^^^^^^^^^^ expected 'Interface-I2', found 'CPointer<Int64>'
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse2.cj:19:18:
   | 
19 | var node21: I2 = CPointer(CPointer<Int64>())
   |                  ^ 
   | 

3 errors generated, 3 errors printed.
*/
