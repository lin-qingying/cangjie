// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class Boo<T> where T <: Int64 {
    public static func foo<V>(x: T, y: V) where V <: Int32 {
        1
    }
}

class Coo <: Boo<Int64> {
    public static redef func foo<V>(x: Int64, y: V) where V <: Int32 {
        2
    }
}

main() {
    var c = Coo.foo<Int32>(0, 0)
    var b = Boo<Int64>.foo<Int32>(0, 0)
    if (c != 2 && b != 1) {
        return 1
    }
    return 0
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
 ==> class_redef_15.cj:9:16:
  | 
9 | open class Boo<T> where T <: Int64 {
  |                ^ 
  | 

error: the upper bound 'Int32' of generic parameter 'Generics-V' must be class or interface
  ==> class_redef_15.cj:10:28:
   | 
10 |     public static func foo<V>(x: T, y: V) where V <: Int32 {
   |                            ^ 
   | 

error: the upper bound 'Int32' of generic parameter 'Generics-V' must be class or interface
  ==> class_redef_15.cj:16:34:
   | 
16 |     public static redef func foo<V>(x: Int64, y: V) where V <: Int32 {
   |                                  ^ 
   | 

3 errors generated, 3 errors printed.
*/
