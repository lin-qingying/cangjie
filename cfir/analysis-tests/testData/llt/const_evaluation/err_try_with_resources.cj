// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A <: Resource {
    public func isClosed() {
        print("A!");
        true
    }
    public func close() {}
}

const a: Unit = try (a: A = A()) {
    0
}

main() {
    print("success!\n")
    return a
}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_try_with_resources.cj:17:17:
   | 
17 |   const a: Unit = try (a: A = A()) {
   |  _________________^
18 | |     0
19 | | }
   | |_^
   | 
   # note: try-with-resources expressions are not constant

1 error generated, 1 error printed.
*/
