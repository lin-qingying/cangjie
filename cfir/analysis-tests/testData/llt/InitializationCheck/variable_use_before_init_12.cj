// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let (c0, c1) = (f0(), f1())

func f0(): Int64 {
    c1
}

func f1(): Int64 {
    c0
}

main() {
    println("${c0} ${c1}")
}

/* SCAN
error: global/static variable 'c1' is used before initialization during initializing 'c1'
  ==> variable_use_before_init_12.cj:12:5:
   | 
12 |     c1
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
