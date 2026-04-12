// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt -Woff=deprecated %f -o %output

internal import std.sync.*

main(): Int64 {
    let a: Int64 = synchronized(ReentrantMutex()) {}
    0
}

/* SCAN
error: mismatched types
  ==> synchronized_test1.cj:12:51:
   | 
12 |     let a: Int64 = synchronized(ReentrantMutex()) {}
   |                                                   ^^ expected 'Int64', found 'Unit'
   | 

1 error generated, 1 error printed.
*/
