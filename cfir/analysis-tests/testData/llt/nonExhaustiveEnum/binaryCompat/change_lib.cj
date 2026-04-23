// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.old.cj %n.new.cj
// not CJNATIVE does not support binary compatibility

import a.*

main() {
    match (m) {
        case B => 0
        case C => 1
        case _ => 2
    }
}
