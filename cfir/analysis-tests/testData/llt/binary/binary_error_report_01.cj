// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %f 2>&1 | compare %f

abstract class Base {
    public static func get(): Int32 {
        return 1
    }
}

class Data <: Base {}

func run(): Int64 {
    var value: Data = Data()
    if (value.get() != 1) {
        return 1
    }
    return 0
}

/* SCAN
error: object cannot access static member 'get'
  ==> binary_error_report_01.cj:19:9:
   | 
19 |     if (value.get() != 1) {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
