// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// The if expression does not have ending 'else' branch, so the entire type must always be the type of unit.
main() {
    var a: Unit = if (true) {
        1
    } else if (false) {
        1.0
    }
    return 0
}
