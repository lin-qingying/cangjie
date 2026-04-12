// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    mut prop x: Int64
}

struct A <: I {
    public mut override prop x: Int64 {
        set(_) {}
    }
}

main() {
    println(A().x)
}

/* SCAN
error: property must implement both getter/setter of interface property 'x'
  ==> err_prop_no_get_01.cj:14:5:
   | 
14 |       public mut override prop x: Int64 {
   |  _____^
15 | |         set(_) {}
16 | |     }
   | |_____^
   | 

1 error generated, 1 error printed.
*/
