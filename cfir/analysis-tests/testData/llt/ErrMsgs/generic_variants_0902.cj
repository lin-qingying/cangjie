// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt  %f  -o %output

package pkg

main(): Int64 {
    if (A<String>().b != 0) {
        return 1
    }
    return 0
}

class A<T> {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
    public var b = match (Data.U) {
        case _: T => 2 // class T{}
        case x: Data => 0
        case _ => 5
    }
}

class T {}

enum Data {
    T | V
}

/* SCAN
error: 'U' is not a member of enum 'Data'
  ==> generic_variants_0902.cj:24:32:
   | 
24 |     public var b = match (Data.U) {
   |                                ^ 
   | 

1 error generated, 1 error printed.
*/
