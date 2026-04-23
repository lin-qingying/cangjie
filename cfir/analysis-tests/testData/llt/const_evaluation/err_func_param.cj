// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

const func test(a: Int64) {
    const b: Int64 = <!EXPECT_CONST!>a<!>
    return b
}

main() {}
