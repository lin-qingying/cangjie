// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.



main() {
    let x = 0
    match (x) {
        case 0 => 0
        case _ => <!TYPE_INCOMPATIBLE!>m<!>atch (x) {
            case 0 => 1
            case _ => "a"
        }
    }
}
