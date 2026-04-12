// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
internal import std.sort.*

main(): Int64 {
    var a1 = [[r'c', r'd'], [r'a', r'b']]
    a1.sort()
    return 1
}

/* SCAN
error: 'sort' is not a member of struct 'Array<Struct-Array<Rune>>'
  ==> generic_constraint8.cj:12:8:
   | 
12 |     a1.sort()
   |        ^^^^ 
   | 

1 error generated, 1 error printed.
*/
