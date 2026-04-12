// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public class Test {
    public Test(private let cond: () -> Bool) {
        cond()
    }
    public const init(cond: Bool) {
        this({=> cond})
    }
    public func test() {
        println("ok")
    }
}

main() {
    Test(true).test()
}

/* SCAN
error: expected 'const' expression
  ==> err_this_03.cj:14:9:
   | 
14 |         this({=> cond})
   |         ^^^^ 
   | 

1 error generated, 1 error printed.
*/
