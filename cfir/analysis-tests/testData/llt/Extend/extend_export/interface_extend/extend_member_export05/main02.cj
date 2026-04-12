// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb1.cj -o b.%middle
// (not CJNATIVE) EXEC-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output
// (CJNATIVE) ERRCHECK: %compiler %cmp_opt b.%middle a.%middle %f -o %output

package b.a

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
  ==> main02.cj:20:18:
   | 
20 |     Foo<Int64>().f1() // error
   |                  ^^ 
   | 

error: 'f2' is not a member of class 'Foo<Int64>'
  ==> main02.cj:21:18:
   | 
21 |     Foo<Int64>().f2() // error
   |                  ^^ 
   | 

error: 'f3' is not a member of class 'Foo<Int64>'
  ==> main02.cj:22:18:
   | 
22 |     Foo<Int64>().f3() // error
   |                  ^^ 
   | 

warning: unused import 'b.c.*'
  ==> main02.cj:16:1:
   | 
16 | import b.c.*
   | ^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 errors generated, 3 errors printed.
1 warning generated, 1 warning printed.
*/
