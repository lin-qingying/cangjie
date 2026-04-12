// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit<T> {
    Day | Year(T)
}

main() {
    var time = Day
    var time1 = Year(1)
    return 1
}

/* SCAN
error: generic type should be used with type argument
  ==> enum4.cj:14:16:
   | 
14 |     var time = Day
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
