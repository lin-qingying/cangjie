// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let f1 = {x: (Any) -> Unit => ()}
let f2 = {x: (Int64) -> Unit => ()}
let fp = {x: Int64 => ()}

main() {
    let f = if (true) {
        f1
    } else {
        f2
    }
    f(fp)
}

/* SCAN
error: no matching function for operator '()' function call
  ==> if_check_type8.cj:19:5:
   | 
19 |     f(fp)
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
