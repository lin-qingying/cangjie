// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %f -o %output 2>&1 | compare %f

class A<T> {
    static func test(): Unit {}
}

func test(a: () -> Unit) {}

main() {
    test(A.test)
}

/* SCAN
error: generic type should be used with type argument
  ==> generic_call_static_impl11.cj:16:10:
   | 
16 |     test(A.test)
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
