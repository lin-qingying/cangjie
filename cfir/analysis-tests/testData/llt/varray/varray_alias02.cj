// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output 2>&1 | compare %f


type varr2 = VArray

main() {
  var c: varr2<Int64, $3> = varr2<Int64, $3>(repeat: 0)
}

/* SCAN
error: expected type parameters after 'VArray' keyword
  ==> varray_alias02.cj:10:20:
   | 
10 | type varr2 = VArray
   |                    ^ 
   | 

error: unclosed delimiter: '<'
  ==> varray_alias02.cj:13:22:
   | 
13 |   var c: varr2<Int64, $3> = varr2<Int64, $3>(repeat: 0)
   |               ~      ^ expected '>' here
   |               |
   |               to match this opening '<'
   | 

error: expected type name after ',', found '$'
  ==> varray_alias02.cj:13:23:
   | 
13 |   var c: varr2<Int64, $3> = varr2<Int64, $3>(repeat: 0)
   |                     ~ ^ expected type name here
   |                     |
   |                     after ','
   | 

error: expected expression or declaration, found '}'
  ==> varray_alias02.cj:14:1:
   | 
14 | }
   | ^ expected expression or declaration here
   | 

4 errors generated, 4 errors printed.
*/
