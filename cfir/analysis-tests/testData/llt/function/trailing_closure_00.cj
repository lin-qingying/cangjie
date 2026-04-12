// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

paCkage use

import defIne.*

main() {
    Return @
    M[x](y)
}

/* SCAN
error: expected declaration, found 'paCkage'
 ==> trailing_closure_00.cj:9:1:
  | 
9 | paCkage use
  | ^^^^^^^ expected declaration here
  | 
  # note: only declarations or macro expressions can be used in the top-level
  # help: did you mean 'package'?

error: unclosed delimiter: '{'
  ==> trailing_closure_00.cj:16:2:
   | 
13 | main() {
   |        ~ to match this opening '{'
14 |     Return @
   | ...
16 | }
   |  ^ expected '}' here
   | 

2 errors generated, 2 errors printed.
*/
