// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

const func f() {
    var x = 0
    x
}

main() {
    f()
}

/* SCAN
error: cannot define 'var' variable in 'const' function
  ==> err_func_var.cj:10:9:
   | 
10 |     var x = 0
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
