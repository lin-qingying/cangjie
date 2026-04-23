// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let v: Int64 = if (match {
        case _ =>
            <!NO_MATCHING_OPERATOR_INVOKE!>Option<Unit>.None<!>()
            true
    }) {
        1
    } else {
        2
    } + 1
    return 0
}
