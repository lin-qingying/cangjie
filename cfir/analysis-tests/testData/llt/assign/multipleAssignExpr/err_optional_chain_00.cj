// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C {}

main() {
    var t: (C, C, C) = (C(), C(), C())
    <!TYPE_MISMATCH!>(<!OPTIONAL_CHAIN_NON_OPTIONAL!>t?[1]<!>, _) = (C(), 1)<!>
}
