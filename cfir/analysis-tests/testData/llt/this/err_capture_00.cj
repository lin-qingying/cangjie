// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    mut func f() {
        false
    }
    S() {
        let g = {=> f()}
        print("${g()}")
    }
}

main() {}

/* SCAN
error: 'this' is not allowed to be captured in constructor of struct
  ==> err_capture_00.cj:14:21:
   | 
14 |         let g = {=> f()}
   |                     ^ 
   | 

1 error generated, 1 error printed.
*/
