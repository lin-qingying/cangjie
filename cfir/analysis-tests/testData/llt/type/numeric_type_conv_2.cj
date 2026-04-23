// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(): Option<Int64> {
    return <!TYPE_MISMATCH!>Int32(4)<!>
}

main(): Int64 {
    f()
    return 0
}
