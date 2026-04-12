// DEPENDENCE: ./other.cj
// EXEC-PIPE: %compiler %cmp_opt %f other.cj -o %output 2>&1 | compare %f

import std.deriving.Derive

main() {
}

/* SCAN
warning: unused import 'std.deriving.Derive'
 ==> unused003.cj:4:1:
  | 
4 | import std.deriving.Derive
  | ^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
