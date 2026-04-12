// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga/pkga.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt pkga.%middle %f  -o %n.%suffix 2>&1 | compare %f

import pkga.C

class C1 <: C<Int32> {}

class C4 <: C<Int64> {}

open class C2<T> <: C<T> {}

class C3 <: C2<Int32> {}

class C5 <: C2<Int64> {}

main() {
    var a1 = C<Int32>()
    var a2 = C<Int64>()
    var b1 = C2<Int32>()
    var b2 = C2<Int64>()
    return 0
}

/* SCAN
error: generic instantiation 'C<Int32>' causes ambiguous function 'c1'
  ==> main.cj:13:13:
   | 
13 | class C1 <: C<Int32> {}
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:17:
note: found candidate
  ==> (package pkga)pkga.cj:13:17:

error: generic instantiation 'C2<Int32>' causes ambiguous function 'c1'
  ==> main.cj:19:13:
   | 
19 | class C3 <: C2<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:17:
note: found candidate
  ==> (package pkga)pkga.cj:13:17:

error: generic instantiation 'C<Int32>' causes ambiguous function 'c1'
  ==> main.cj:24:14:
   | 
24 |     var a1 = C<Int32>()
   |              ^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:17:
note: found candidate
  ==> (package pkga)pkga.cj:13:17:

error: generic instantiation 'C2<Int32>' causes ambiguous function 'c1'
  ==> main.cj:26:14:
   | 
26 |     var b1 = C2<Int32>()
   |              ^^^^^^^^^ 
   | 
note: found candidate
  ==> (package pkga)pkga.cj:10:17:
note: found candidate
  ==> (package pkga)pkga.cj:13:17:

4 errors generated, 4 errors printed.
*/
