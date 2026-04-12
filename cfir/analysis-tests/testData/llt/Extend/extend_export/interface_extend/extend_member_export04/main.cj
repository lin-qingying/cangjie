// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o b.%middle
// EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

package b.c

import a.*
import b.*

main() {
    Foo().f1() // error
    Foo().f1(1) // ok
    Foo().f2() // ok
}

/* SCAN
error: missing argument for parameter list '(Int64)' in call
  ==> main.cj:18:13:
   | 
18 |     Foo().f1() // error
   |             ^^ expected 1 argument, found 0
   | 
note: found candidate
  ==> (package b)testb.cj:22:17:

1 error generated, 1 error printed.
*/
