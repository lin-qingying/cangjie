// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


<!sema_typealias_cycle!>t<!>ype D = F

<!sema_typealias_cycle!>t<!>ype F = D

main() {
    return 0
}
