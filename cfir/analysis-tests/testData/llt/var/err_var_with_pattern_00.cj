// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let _ = 'c' + unsafe {
        let _ = "${x}"
    }
}

/* SCAN
error: undeclared identifier 'x'
  ==> err_var_with_pattern_00.cj:11:20:
   | 
11 |         let _ = "${x}"
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
