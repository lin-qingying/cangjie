// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var a = "aaa"
    var b = 3 + 4 + 5 <!INVALID_BINARY_OPERATOR!>+<!> a + 5 + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 +
        a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a
    0
}
