// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb5.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb5.cj -o b.%middle
// EXEC-PIPE-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %f

package b.c.d

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().f1() // error
    Foo<Int64>().f2() // ok
    Foo<Int64>().f3() // ok
    Foo<Int64>().f4() // ok
}

/* SCAN
error: no matching function declaration for function call 'f1'
  ==> main10.cj:19:18:
   | 
19 |     Foo<Int64>().f1() // error
   |                  ^ 
   | 
note: to use the following extension, import 'Interface-I1'
  ==> (package b.c)testb5.cj:27:1:

1 error generated, 1 error printed.
*/
