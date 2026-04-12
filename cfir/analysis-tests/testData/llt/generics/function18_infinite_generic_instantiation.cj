// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class Base<T> {}

func buildList<T>(n: Int32): Int64 {
    if (n > 0) {
        buildList<Base<T>>(n - 1)
    } else {
        buildList<Base<T>>(0)
    }
    return 0
}

main(): Int64 {
    return buildList<Int32>(10)
}

/* SCAN
error: generic infinite instantiation
  ==> function18_infinite_generic_instantiation.cj:13:9:
   | 
13 |         buildList<Base<T>>(n - 1)
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
