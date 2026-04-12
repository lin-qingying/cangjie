// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var a = Object()[1, r'c']
}

/* SCAN
error: invalid subscript operator [] on type 'Class-Object' with index types 'Int64', Rune'
  ==> subscript_02.cj:10:13:
   | 
10 |     var a = Object()[1, r'c']
   |             ^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func [](index0: Int64, index1: Rune)' for type 'Class-Object'

1 error generated, 1 error printed.
*/
