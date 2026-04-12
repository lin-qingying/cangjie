// EXEC-PIPE: %compiler %cmp_opt --no-sub-pkg %f -o %output 2>&1 | compare %f

internal import std.collection.{ArrayList, Map}
main() {}

/* SCAN
warning: unused import 'std.collection.ArrayList'
 ==> unused007.cj:3:33:
  | 
3 | internal import std.collection.{ArrayList, Map}
  |                                 ^^^^^^^^^ unused import
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused import 'std.collection.Map'
 ==> unused007.cj:3:44:
  | 
3 | internal import std.collection.{ArrayList, Map}
  |                                            ^^^ unused import
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
