// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct R {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    var c: Int32 = 0
    let b = <!TYPE_MISMATCH!><!INVISIBLE_MEMBER!>R().a<!><!>
}
