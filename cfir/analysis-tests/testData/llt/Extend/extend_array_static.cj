// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend<T> Array<T> {
    public static prop ns: Int64 {
        get() {
            return 0
        }
    }
}

main(): Int64 {
    return Array.ns
}

/* SCAN
error: generic type should be used with type argument
  ==> extend_array_static.cj:18:12:
   | 
18 |     return Array.ns
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
