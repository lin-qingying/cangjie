// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

type B<T> = Cl<T>

func run<T>(a: T): Int64 {
    var b: B<T>
    return 0
}

main(): Int64 {
    return run<Int64>(1)
}

/* SCAN
error: undeclared type name 'Cl'
 ==> typealias13.cj:9:13:
  | 
9 | type B<T> = Cl<T>
  |             ^ 
  | 

1 error generated, 1 error printed.
*/
