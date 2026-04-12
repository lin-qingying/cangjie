// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o testa.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o testb.%middle
// EXEC-PIPE-1: %compiler %cmp_opt testb.%middle testa.%middle %f -o %output 2>&1 | compare %f

import a.*
import b.I1

public class B <: A & IChild {}

main() {}

/* SCAN
warning: unused import 'b.I1'
  ==> main.cj:13:1:
   | 
13 | import b.I1
   | ^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: class 'B' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> main.cj:15:1:
   | 
15 | public class B <: A & IChild {}
   | ^ 
   | 
note: unimplemented interface function 'some'
  ==> (package a)testa.cj:10:5:

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
