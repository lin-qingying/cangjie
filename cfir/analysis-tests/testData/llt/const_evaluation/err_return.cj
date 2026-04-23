// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    const y = match (0) {
        case _ where <!EXPECT_CONST!>return<!> => 0
        case _ => 1
    }
}

main() {}
