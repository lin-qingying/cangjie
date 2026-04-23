// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Day {
    public let width: Int32 = 1
    public let length: Int32 = 2
    public let perimeter2: (Unit) -> Int32 = <!TYPE_MISMATCH!>{=> var res: Int32 = 0}<!>
}
