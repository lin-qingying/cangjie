// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f


import std.collection.ArrayList as mylist
import std.collection.ArrayList
import std.collection
import std.collection as mycol

main(): Int64 {
    var a = collection.ArrayList<Int64>(10)
    return a.size
}

/* SCAN
warning: unused import 'std.collection.ArrayList as mylist'
  ==> unused015.cj:10:1:
   | 
10 | import std.collection.ArrayList as mylist
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused import 'std.collection as mycol'
  ==> unused015.cj:13:1:
   | 
13 | import std.collection as mycol
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/

