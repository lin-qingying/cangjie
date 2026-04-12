// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let f: (Float32) -> Unit = {_ =>}
    (0.0 / 1.0) - x |> f
}

/* SCAN
error: undeclared identifier 'x'
  ==> pipeline10.cj:11:19:
   | 
11 |     (0.0 / 1.0) - x |> f
   |                   ^ 
   | 

1 error generated, 1 error printed.
*/
