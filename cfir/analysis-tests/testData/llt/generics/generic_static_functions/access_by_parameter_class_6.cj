// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class C {
    private static let a = 0
}

func Foo<U>(a: U) where U <: C {
    var b = U.a
    return b
}

class C1 <: C {}

main() {
    return Foo<C1>(C1())
}

/* SCAN
error: can not access field 'a'
  ==> access_by_parameter_class_6.cj:14:13:
   | 
14 |     var b = U.a
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
