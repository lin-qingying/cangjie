// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct d {
    public init() {
        match {
            case _ => aaaa
        }
    }
}

/* SCAN
error: undeclared identifier 'aaaa'
  ==> match_no_selector_008.cj:12:23:
   | 
12 |             case _ => aaaa
   |                       ^ 
   | 

1 error generated, 1 error printed.
*/
