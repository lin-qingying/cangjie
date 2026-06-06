// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %noColor %f -o %output

main() {
    let Int_literal:Int33 = 1<!lex_unknown_suffix!>suffix<!>;
    return 0;
}
