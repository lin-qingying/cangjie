// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：宽数组字面量（单行 100 个元素、10x10 二维数组、30 元素字符串数组、5x3x4 三层嵌套数组），
// 用于覆盖构树时数组字面量的元素列表、嵌套数组表达式与多层下标。注意：无类型的二维数组字面量会被官方推断成展平的一维
// Array<Int64>（见 recovery/flatNestedArrayLiteral.cj）；二维数组的连续下标链 `a[0][0]` 在官方 1.0.5 / 1.1.3 的静态库
// 目标上会让 LLVM opt 校验崩溃，因此这里只用整体访问与三层嵌套的下标链。VArray 的元素类型不能是数组，见 recovery/varrayNestedArray.cj。
// cjc 1.0.5 / 1.1.3 零诊断。
func wideArrayLiterals(): Int64 {
    let flat = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100]
    let grid = [
        [11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
        [21, 22, 23, 24, 25, 26, 27, 28, 29, 30],
        [31, 32, 33, 34, 35, 36, 37, 38, 39, 40],
        [41, 42, 43, 44, 45, 46, 47, 48, 49, 50],
        [51, 52, 53, 54, 55, 56, 57, 58, 59, 60],
        [61, 62, 63, 64, 65, 66, 67, 68, 69, 70],
        [71, 72, 73, 74, 75, 76, 77, 78, 79, 80],
        [81, 82, 83, 84, 85, 86, 87, 88, 89, 90],
        [91, 92, 93, 94, 95, 96, 97, 98, 99, 100],
        [101, 102, 103, 104, 105, 106, 107, 108, 109, 110],
    ]
    let texts = [
        "ssssssssssssssssssss1",
        "ssssssssssssssssssss2",
        "ssssssssssssssssssss3",
        "ssssssssssssssssssss4",
        "ssssssssssssssssssss5",
        "ssssssssssssssssssss6",
        "ssssssssssssssssssss7",
        "ssssssssssssssssssss8",
        "ssssssssssssssssssss9",
        "ssssssssssssssssssss10",
        "ssssssssssssssssssss11",
        "ssssssssssssssssssss12",
        "ssssssssssssssssssss13",
        "ssssssssssssssssssss14",
        "ssssssssssssssssssss15",
        "ssssssssssssssssssss16",
        "ssssssssssssssssssss17",
        "ssssssssssssssssssss18",
        "ssssssssssssssssssss19",
        "ssssssssssssssssssss20",
        "ssssssssssssssssssss21",
        "ssssssssssssssssssss22",
        "ssssssssssssssssssss23",
        "ssssssssssssssssssss24",
        "ssssssssssssssssssss25",
        "ssssssssssssssssssss26",
        "ssssssssssssssssssss27",
        "ssssssssssssssssssss28",
        "ssssssssssssssssssss29",
        "ssssssssssssssssssss30",
    ]
    let cube = [[
        [
            [111, 112, 113, 114],
            [121, 122, 123, 124],
            [131, 132, 133, 134],
        ],
        [
            [211, 212, 213, 214],
            [221, 222, 223, 224],
            [231, 232, 233, 234],
        ],
        [
            [311, 312, 313, 314],
            [321, 322, 323, 324],
            [331, 332, 333, 334],
        ],
        [
            [411, 412, 413, 414],
            [421, 422, 423, 424],
            [431, 432, 433, 434],
        ],
        [
            [511, 512, 513, 514],
            [521, 522, 523, 524],
            [531, 532, 533, 534],
        ],
    ]]
    let cubeCell = cube[0]
    let firstCell = flat[0]
    let lastCell = flat[99]
    let sum = flat[0] + flat[99] + firstCell + lastCell + texts[29].size
    return sum
}
