// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    while (true) {
        const x = match (0) {
            case _ where continue => 0
            case _ => 1
        }
    }
}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_continue.cj:12:26:
   | 
12 |             case _ where continue => 0
   |                          ^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
