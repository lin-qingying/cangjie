// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f


main(): Int64 {
    var a: Array<String> = ["aa"]
    var z = a.clone
    a.size = 1
    var x = a.clone(2)
    return 0
}

/* SCAN
error: ambiguous use of 'clone'
  ==> array_invalid_api.cj:12:13:
   | 
12 |     var z = a.clone
   |             ^^^^^^^ 
   | 
note: found function member of 'Struct-Array<Generics-T>'
   ==> (package std.core)array.cj:273:17:
note: found function member of 'Struct-Array<Generics-T>'
   ==> (package std.core)array.cj:287:17:

error: cannot convert an integer literal to type 'Struct-Range<Int64>'
  ==> array_invalid_api.cj:14:21:
   | 
14 |     var x = a.clone(2)
   |                     ^ 
   | 

error: cannot assign to immutable value
  ==> array_invalid_api.cj:13:5:
   | 
13 |     a.size = 1
   |     ^^^^^^^^^^ 
   | 
note: property 'size' is immutable
  ==> (package std.core)array_common.cj:60:17:

3 errors generated, 3 errors printed.
*/
