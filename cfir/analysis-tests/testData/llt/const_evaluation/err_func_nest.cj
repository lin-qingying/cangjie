// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

// f(x) = 2 * x + 1
const func f(x: Int64) {
    func g(x: Int64) {
        2 * x
    }
    g(x) + 1
}

const x = f(3)

main() {
    x
}

/* SCAN
error: expected 'const' function
  ==> err_func_nest.cj:11:10:
   | 
11 |     func g(x: Int64) {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
