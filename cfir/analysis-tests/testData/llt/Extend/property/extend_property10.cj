// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() : Int64 {
    return 0
}

extend String {
    public mut prop m: Int32
    <!parse_expected_character!>public<!> static prop n: Int32
<!parse_expected_character!>}<!>
