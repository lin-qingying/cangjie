// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class Boo<T> where T <: Int64 {
    public static func foo<V>(x: T, y: V) {
        x + 1
    }
}

class Coo <: Boo<Int64> {
    public static redef func foo<V>(x: Int64, y: V) {
        x + 2
    }
}

main() {
    var c = Coo.foo<Int64>(0, 0)
    if (c != 2) {
        return 1
    }
    return 0
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
 ==> class_redef_14.cj:9:16:
  | 
9 | open class Boo<T> where T <: Int64 {
  |                ^ 
  | 

1 error generated, 1 error printed.
*/
