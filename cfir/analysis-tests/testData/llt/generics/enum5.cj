// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit<T> {
    Year(T) | Month(T)
}

var time = TimeUnit<Int32>.Year<Int64>(4)
var res1: Int32 = match (time) {
    case Year(x) => 2020
    case Month(x) => 9
}

func run(): Int64 {
    if (res1 == 2020) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: type arguments cannot appear after 'Year' when enum type 'TimeUnit' is given
  ==> enum5.cj:13:12:
   | 
13 | var time = TimeUnit<Int32>.Year<Int64>(4)
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
