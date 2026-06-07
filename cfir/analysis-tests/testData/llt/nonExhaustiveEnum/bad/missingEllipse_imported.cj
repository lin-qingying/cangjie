// FILE: missingEllipse_imported.lib.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public enum A<T> {
    | B | ...
}

// FILE: missingEllipse_imported.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import a.*

main() {
    let a = A<Int64>.B
    <!NON_EXHAUSTIVE_MATCH!>match (a) {
        case B => 0
    }<!>
}
