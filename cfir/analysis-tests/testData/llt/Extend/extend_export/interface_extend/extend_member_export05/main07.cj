// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb4.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb4.cj -o b.%middle
// EXEC-PIPE-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %f

package d

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().f1() // error
    Foo<Int64>().f2() // error
    Foo<Int64>().f3() // error
}

/* SCAN
error: 'f1' is not a member of class 'Foo<Int64>'
  ==> main07.cj:19:18:
   | 
19 |     Foo<Int64>().f1() // error
   |                  ^^ 
   | 
note: to use the following extension, you must import at least one of its inherited interfaces
  ==> (package b.c)testb4.cj:27:1:

error: 'f2' is not a member of class 'Foo<Int64>'
  ==> main07.cj:20:18:
   | 
20 |     Foo<Int64>().f2() // error
   |                  ^^ 
   | 
note: to use the following extension, you must import at least one of its inherited interfaces
  ==> (package b.c)testb4.cj:27:1:

error: 'f3' is not a member of class 'Foo<Int64>'
  ==> main07.cj:21:18:
   | 
21 |     Foo<Int64>().f3() // error
   |                  ^^ 
   | 
note: to use the following extension, you must import at least one of its inherited interfaces
  ==> (package b.c)testb4.cj:27:1:

warning: unused import 'b.c.*'
  ==> main07.cj:15:1:
   | 
15 | import b.c.*
   | ^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 errors generated, 3 errors printed.
1 warning generated, 1 warning printed.
*/
