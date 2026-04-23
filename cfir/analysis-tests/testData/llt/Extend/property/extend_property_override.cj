// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    prop pa: Int64 {
        get() {
            1
        }
    }
}

extend Int32 <: I {
    public prop pa: Int64 {
        get() {
            0
        }
    }
}

main() {
    return 1.pa
}
