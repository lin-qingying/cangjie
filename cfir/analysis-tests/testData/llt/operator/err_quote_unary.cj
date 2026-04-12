// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

import std.ast.*

main() {
    quote($(-""))
}

/* SCAN
error: invalid unary operator '-' on type 'Struct-String'
  ==> err_quote_unary.cj:12:13:
   | 
12 |     quote($(-""))
   |             ^^^ 
   | 
   # note: you may want to implement 'operator func -()' for type 'Struct-String'

1 error generated, 1 error printed.
*/
 