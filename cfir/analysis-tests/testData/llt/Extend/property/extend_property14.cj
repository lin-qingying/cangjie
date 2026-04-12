// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    if (foo<Int64>(1) != 3) {
        return 1
    }
    return 0
}

func foo<T>(x: T): Int32 where T <: Int64 {
    return x.n + x.m
}

extend Int64 {
    public prop m: Int32 {
        get() {
            return 2
        }
    }
    public prop n: Int32 {
        get() {
            return 1
        }
    }
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_property14.cj:16:10:
   | 
16 | func foo<T>(x: T): Int32 where T <: Int64 {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
