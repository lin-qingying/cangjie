// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f1() {
    @IfAvailable(APILevel:11, { => println(0) }, { => println(0) }<!parse_expected_right_delimiter!>,<!> { => println(0) })
}

main() {
}
