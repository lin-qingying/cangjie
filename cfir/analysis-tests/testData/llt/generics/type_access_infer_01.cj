// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A<T> {
    static var c = 5
}

main() {
    try {
        A.c >>= 0
    } catch (d: NoneValueException) {}
}

/* SCAN
error: generic type should be used with type argument
  ==> type_access_infer_01.cj:15:9:
   | 
15 |         A.c >>= 0
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
