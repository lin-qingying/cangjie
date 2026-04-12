// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func benchmark(): Unit {
    let f: (Int64) -> Unit
    f = {
        n: Int64 => var res = spawn {
            => f(n - 1)
        }
    }
    f(4)
}

main() {
    benchmark()
}

/* SCAN
error: cannot capture variable 'f' before initialization
  ==> lambda_spawn.cj:13:16:
   | 
13 |             => f(n - 1)
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
