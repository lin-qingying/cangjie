// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func run(): Int64 {
    var s1: String = ""
    var s2: String = "1_23"

    if (!s1.startsWith("") || s1.startsWith("8") || !s2.startsWith?("")) {
        return 1
    }

    return 0
}

main(): Int64 {
    return run()
}

/* SCAN
error: cannot use optional chaining
  ==> err_unary_00.cj:13:54:
   | 
13 |     if (!s1.startsWith("") || s1.startsWith("8") || !s2.startsWith?("")) {
   |                                                      ^^^^^^^^^^^^^^^^^^ cannot use optional chaining on non-optional value of type '(Struct-String) -> Bool'
   | 

1 error generated, 1 error printed.
*/
