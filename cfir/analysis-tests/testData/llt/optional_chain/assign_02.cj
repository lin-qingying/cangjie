// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var a = Some([0, 1])
    a?[0]++
    match (a?[0]) {
        case Some(t) => t - 1
        case None => 1
    }
}
