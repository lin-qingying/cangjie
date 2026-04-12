// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A <: ToString & Resource {
    public func toString() {
        "A"
    }
    public func close() {}
    public func isClosed() {
        false
    }
}

main() {
    try (x = A()) {
        let x = 0
        println(x)
    }
}

/* SCAN
error: redefinition of declaration 'x'
  ==> err_scope_01.cj:21:13:
   | 
21 |         let x = 0
   |             ^ 
   | 
note: 'x' is previously declared here
  ==> err_scope_01.cj:20:10:
   | 
20 |     try (x = A()) {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
