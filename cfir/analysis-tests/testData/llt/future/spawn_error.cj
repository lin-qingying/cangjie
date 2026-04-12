// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

func add(a: String): String {
    return a
}

main(): Int64 {
    let fut1: Future<String> = spawn {
        return add,"cang")
        return add("")
    }
    return 0
}

/* SCAN
error: expected ';' or '<NL>', found ','
  ==> spawn_error.cj:15:19:
   | 
15 |         return add,"cang")
   |                   ^ expected ';' or '<NL>' here
   | 

1 error generated, 1 error printed.
*/
