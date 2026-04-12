// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

import define.*

main() {
    var a = @SAME{1}() <= @SAME{1}()
    var b = @SAME[0]{1}() <= @SAME[0]{1}()
    if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
    {
        return 0
    }
    else
    {
        return 1
    }
}

/* SCAN
error: expected declaration, found '{'
  ==> timeout_02.cj:13:18:
   | 
13 |     var a = @SAME{1}() <= @SAME{1}()
   |                  ^ expected declaration here
   | 

error: expected declaration, found '{'
  ==> timeout_02.cj:13:32:
   | 
13 |     var a = @SAME{1}() <= @SAME{1}()
   |                                ^ expected declaration here
   | 

error: expected declaration, found '{'
  ==> timeout_02.cj:14:21:
   | 
14 |     var b = @SAME[0]{1}() <= @SAME[0]{1}()
   |                     ^ expected declaration here
   | 

error: expected declaration, found '{'
  ==> timeout_02.cj:14:38:
   | 
14 |     var b = @SAME[0]{1}() <= @SAME[0]{1}()
   |                                      ^ expected declaration here
   | 

error: unclosed delimiter: '('
  ==> timeout_02.cj:15:208:
   | 
15 |     if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
   |                                                                                                                                                                                 ~                              ^ expected ')' here
   |                                                                                                                                                                                 |
   |                                                                                                                                                                                 to match this opening '('
   | 

error: unclosed delimiter: '('
  ==> timeout_02.cj:18:7:
   | 
15 |     if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
   |                                                                                                                                                                                ~ to match this opening '('
16 |     {
   | ...
18 |     }
   |       ^ expected ')' here
   | 

error: unclosed delimiter: '('
  ==> timeout_02.cj:19:10:
   | 
15 |     if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
   |                                                                                                                                                                          ~ to match this opening '('
16 |     {
   | ...
19 |     else
   |          ^ expected ')' here
   | 

error: unclosed delimiter: '('
  ==> timeout_02.cj:22:7:
   | 
15 |     if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
   |                                                                                                                                                                         ~ to match this opening '('
16 |     {
   | ...
22 |     }
   |       ^ expected ')' here
   | 

error: unclosed delimiter: '('
  ==> timeout_02.cj:23:3:
   | 
15 |     if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
   |                                                                                                                                                                   ~ to match this opening '('
16 |     {
   | ...
23 | }
   |   ^ expected ')' here
   | 

9 errors generated, 9 errors printed.
*/
