// EXEC-PIPE: %compiler %cmp_opt --no-sub-pkg %f -o %output 2>&1 | compare %f

internal import std.collection.ArrayList as b
main() {}

/* SCAN
warning: unused import 'std.collection.ArrayList as b'
 ==> unused002.cj:3:1:
  | 
3 | internal import std.collection.ArrayList as b
  | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
