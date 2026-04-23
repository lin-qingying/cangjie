// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let t1 = (1, 2);
    if (t1 <!INVALID_BINARY_OPERATOR!>!=<!> (1, 2, 3)) {
        return 0;
    }
    return 1;
}
