// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

struct Rectangle {
    public let width: Int64
    public let length: Int64

    public init () {
        this.width = 10
        this.length = 20
    }

    public init(width: Int64, length: Int64) {
        <!parse_expected_expr_or_decl_in!>init<!>()
        this.width = width
        this.length = length
    }
}

main(): Int64 {
    let rec = Rectangle()
    rec = Rectangle(100, 200)
    return 0
}
