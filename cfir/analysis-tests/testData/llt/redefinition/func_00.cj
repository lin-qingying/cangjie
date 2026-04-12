// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x: Bool) {
    println(x)
    let x = 0
    println(x)
}

main() {
    f(false)
}

/* SCAN
error: redefinition of declaration 'x'
  ==> func_00.cj:11:9:
   | 
11 |     let x = 0
   |         ^ 
   | 
note: 'x' is previously declared here
 ==> func_00.cj:9:8:
  | 
9 | func f(x: Bool) {
  |        ^ 
  | 

1 error generated, 1 error printed.
*/
