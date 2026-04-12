// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

enum E<T0, T1> {
    EE
}

class A<T> {
    static var a = 1
}

main() {
    A<Option<E>>.a <<= 2
    return 1
}

/* SCAN
error: generic type should be used with type argument
  ==> generic_parameters9.cj:18:14:
   | 
18 |     A<Option<E>>.a <<= 2
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
