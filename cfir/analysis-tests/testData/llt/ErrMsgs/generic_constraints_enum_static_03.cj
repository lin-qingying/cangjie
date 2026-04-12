// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt  %f  -o %output

main(): Int64 {
    if (B<Base>().a != "a") {
        return 2
    }
    return 0
}

class B<T> where T <: Base {
    public var b: String = ""
    public var a = T.m2
}

enum Base {
    A | B

    public static prop m: String {
        get() {
            return "a"
        }
    }
}

/* SCAN
error: the upper bound 'Enum-Base' of generic parameter 'Generics-T' must be class or interface
  ==> generic_constraints_enum_static_03.cj:16:9:
   | 
16 | class B<T> where T <: Base {
   |         ^ 
   | 

error: 'm2' is not a static member of exposed generic parameter 'T'
  ==> generic_constraints_enum_static_03.cj:18:20:
   | 
18 |     public var a = T.m2
   |                    ^ 
   | 

2 errors generated, 2 errors printed.
*/
