// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj 2>&1 | compare %f

func foo(): Int32 {
    match (_) {
        case _ => 0
    }
    return 1
}

/* SCAN
error: unexpected _ wildcard
  ==> matchcase6.cj:10:12:
   | 
10 |     match (_) {
   |            ^ expected expression, found wildcard
   | 

1 error generated, 1 error printed.
*/
