// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var g = 0

struct S {
    var x: Int64
    const init(x: Int64) {
        this.x = x
        g = 1
        g++
    }
}

main() {
    const s = S(-1)
    s.x
}

/* SCAN
error: expected 'const' expression
  ==> err_assign.cj:15:9:
   | 
15 |         g = 1
   |         ^^^^^ 
   | 

error: expected 'const' expression
  ==> err_assign.cj:16:9:
   | 
16 |         g++
   |         ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
