// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func r(a: Future<Unit>): Bool {
    r(spawn {
        =>
        if (true) {
            return false
        };
        true
    })
    return true
}

main() {
    0
}

/* SCAN
error: mismatched types
  ==> lambda07.cj:10:13:
   | 
10 |       r(spawn {
   |  _____________^
11 | |         =>
   | | ...
16 | |     })
   | |_____^ expected '() -> Unit', found '() -> Bool'
   | 

1 error generated, 1 error printed.
*/
