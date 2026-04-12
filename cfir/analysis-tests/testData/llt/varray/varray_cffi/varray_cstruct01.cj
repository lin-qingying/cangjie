// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct A {}

@C
struct S {
    var a = VArray<VArray<VArray<VArray<A, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
    var b = VArray<A, $1>(repeat: A()) // error
}

main() {}

/* SCAN
error: member variable 'a' of struct 'S' with @C must be instantiated with CType
  ==> varray_cstruct01.cj:13:9:
   | 
13 |     var a = VArray<VArray<VArray<VArray<A, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
   |         ^ 
   | 

error: member variable 'b' of struct 'S' with @C must be instantiated with CType
  ==> varray_cstruct01.cj:14:9:
   | 
14 |     var b = VArray<A, $1>(repeat: A()) // error
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
