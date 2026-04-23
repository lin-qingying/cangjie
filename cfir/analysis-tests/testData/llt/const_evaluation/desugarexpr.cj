// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

const str1: String = "hello"
var str2: String = "world"

const func test() : Unit {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>str1 += "."<!>
    <!EXPECT_CONST!>str2 += "!"<!>
}

main() {}
