// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: A.cj B.cj C.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt A.cj -o A.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt B.cj -o B.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt C.cj -o C.%middle
// EXEC-PIPE-1: %compiler %cmp_opt A.%middle B.%middle C.%middle %f 2>&1 | compare %f

import A.Foo
import B.I1
import C.I2

main(): Int64 {
    let a = Foo()
    a.f()
    return 1
}

/* SCAN
error: ambiguous match for function call 'f'
  ==> main.cj:19:7:
   | 
19 |     a.f()
   |       ^ 
   | 
note: found extended function member of 'Class-Foo'
  ==> (package B)B.cj:16:17:
note: found extended function member of 'Class-Foo'
  ==> (package C)C.cj:16:17:

error: extend member 'f' is not allowed to shadow members of 'extend Foo'
  ==> (package B)B.cj:16:17:

error: extend member 'f' is not allowed to shadow members of 'extend Foo'
  ==> (package C)C.cj:16:17:

3 errors generated, 3 errors printed.
*/
