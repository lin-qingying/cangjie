// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public class Test {
    public Test(private let cond: () -> Bool) {
        cond()
    }
    public const init(cond: Bool) {
        <!EXPECT_CONST!>this({=> cond})<!>
    }
    public func test() {
        println("ok")
    }
}

main() {
    Test(true).test()
}
