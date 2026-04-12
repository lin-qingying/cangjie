// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %noColor %n.cj -o %output 2>&1 | compare %f

main() {
    var a = 1i16_ 
}

/* SCAN
error: expected ';' or '<NL>', found '_'
  ==> 006.cj:10:17:
   | 
10 |     var a = 1i16_ 
   |                 ^ expected ';' or '<NL>' here
   | 

error: expected expression or declaration, found '}'
  ==> 006.cj:11:1:
   | 
11 | }
   | ^ expected expression or declaration here
   | 

2 errors generated, 2 errors printed.
*/
