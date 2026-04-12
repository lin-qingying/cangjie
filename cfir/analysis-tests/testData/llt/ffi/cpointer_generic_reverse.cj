// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

type C<T> = CPointer<T>

var node4_2: C = C<Int64>() // error.

type D = C<Int64>

var node5_2: C = D() // error.

foreign func malloc(size: UIntNative): CPointer<Unit>

var node17 = CPointer(malloc) // error.
var node8: CPointer = CPointer<Unit>() // error.
var node9: CType = CPointer() // error.
var node14: CString = CPointer() // error.
var node15 = CPointer() // error.
var node16: CPointer = CPointer() // error.

var node13 = CPointer(CPointer<Unit>()) // error.

main() {}

/* SCAN
error: generic type should be used with type argument
  ==> cpointer_generic_reverse.cj:11:14:
   | 
11 | var node4_2: C = C<Int64>() // error.
   |              ^ 
   | 

error: generic type should be used with type argument
  ==> cpointer_generic_reverse.cj:15:14:
   | 
15 | var node5_2: C = D() // error.
   |              ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:19:14:
   | 
19 | var node17 = CPointer(malloc) // error.
   |              ^ 
   | 

error: generic type should be used with type argument
  ==> cpointer_generic_reverse.cj:20:12:
   | 
20 | var node8: CPointer = CPointer<Unit>() // error.
   |            ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:21:20:
   | 
21 | var node9: CType = CPointer() // error.
   |                    ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:22:23:
   | 
22 | var node14: CString = CPointer() // error.
   |                       ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:23:14:
   | 
23 | var node15 = CPointer() // error.
   |              ^ 
   | 

error: generic type should be used with type argument
  ==> cpointer_generic_reverse.cj:24:13:
   | 
24 | var node16: CPointer = CPointer() // error.
   |             ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:24:24:
   | 
24 | var node16: CPointer = CPointer() // error.
   |                        ^ 
   | 

error: 'CPointer' generic type cannot be inferred
  ==> cpointer_generic_reverse.cj:26:14:
   | 
26 | var node13 = CPointer(CPointer<Unit>()) // error.
   |              ^ 
   | 

10 errors generated, 10 errors printed.
*/
