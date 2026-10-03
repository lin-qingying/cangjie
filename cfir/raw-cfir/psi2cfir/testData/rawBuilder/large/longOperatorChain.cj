// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：200 项的长运算链（算术 + 位运算 + 移位混合，移位量取 1..16 以避开官方 ConstAnalysis 的
// chir_shift_length_overflow）、链式成员访问、链式下标、一元运算与超长条件表达式。cjc 1.0.5 / 1.1.3 零诊断。
class ChainHost {
    private let values: Array<Int64> = [0, 1, 2, 3, 4]

    public init() {}

    public func at(i: Int64): Int64 {
        return values[i]
    }

    public func next(i: Int64): ChainHost {
        return this
    }

    public static func of(i: Int64): ChainHost {
        return ChainHost()
    }
}

func longOperatorChain(flag: Bool): Int64 {
    var acc: Int64 = 1
    let host = ChainHost.of(1).next(2)
    let cell = host.at(0) + valuesOf()[0]
    acc + 1
    acc - 2
    acc * 3
    acc & 4
    acc | 5
    acc ^ 6
    acc << 7
    acc >> 8
    acc % 9
    acc / 10
    acc + 11
    acc - 12
    acc * 13
    acc & 14
    acc | 15
    acc ^ 16
    acc << 1
    acc >> 2
    acc % 3
    acc / 4
    acc + 21
    acc - 22
    acc * 23
    acc & 24
    acc | 25
    acc ^ 26
    acc << 11
    acc >> 12
    acc % 13
    acc / 14
    acc + 31
    acc - 32
    acc * 33
    acc & 34
    acc | 35
    acc ^ 36
    acc << 5
    acc >> 6
    acc % 7
    acc / 8
    acc + 41
    acc - 42
    acc * 43
    acc & 44
    acc | 45
    acc ^ 46
    acc << 15
    acc >> 16
    acc % 1
    acc / 2
    acc + 51
    acc - 52
    acc * 53
    acc & 54
    acc | 55
    acc ^ 56
    acc << 9
    acc >> 10
    acc % 11
    acc / 12
    acc + 61
    acc - 62
    acc * 63
    acc & 64
    acc | 65
    acc ^ 66
    acc << 3
    acc >> 4
    acc % 5
    acc / 6
    acc + 71
    acc - 72
    acc * 73
    acc & 74
    acc | 75
    acc ^ 76
    acc << 13
    acc >> 14
    acc % 15
    acc / 16
    acc + 81
    acc - 82
    acc * 83
    acc & 84
    acc | 85
    acc ^ 86
    acc << 7
    acc >> 8
    acc % 9
    acc / 10
    acc + 91
    acc - 92
    acc * 93
    acc & 94
    acc | 95
    acc ^ 96
    acc << 1
    acc >> 2
    acc % 3
    acc / 4
    acc + 101
    acc - 102
    acc * 103
    acc & 104
    acc | 105
    acc ^ 106
    acc << 11
    acc >> 12
    acc % 13
    acc / 14
    acc + 111
    acc - 112
    acc * 113
    acc & 114
    acc | 115
    acc ^ 116
    acc << 5
    acc >> 6
    acc % 7
    acc / 8
    acc + 121
    acc - 122
    acc * 123
    acc & 124
    acc | 125
    acc ^ 126
    acc << 15
    acc >> 16
    acc % 1
    acc / 2
    acc + 131
    acc - 132
    acc * 133
    acc & 134
    acc | 135
    acc ^ 136
    acc << 9
    acc >> 10
    acc % 11
    acc / 12
    acc + 141
    acc - 142
    acc * 143
    acc & 144
    acc | 145
    acc ^ 146
    acc << 3
    acc >> 4
    acc % 5
    acc / 6
    acc + 151
    acc - 152
    acc * 153
    acc & 154
    acc | 155
    acc ^ 156
    acc << 13
    acc >> 14
    acc % 15
    acc / 16
    acc + 161
    acc - 162
    acc * 163
    acc & 164
    acc | 165
    acc ^ 166
    acc << 7
    acc >> 8
    acc % 9
    acc / 10
    acc + 171
    acc - 172
    acc * 173
    acc & 174
    acc | 175
    acc ^ 176
    acc << 1
    acc >> 2
    acc % 3
    acc / 4
    acc + 181
    acc - 182
    acc * 183
    acc & 184
    acc | 185
    acc ^ 186
    acc << 11
    acc >> 12
    acc % 13
    acc / 14
    acc + 191
    acc - 192
    acc * 193
    acc & 194
    acc | 195
    acc ^ 196
    acc << 5
    acc >> 6
    acc % 7
    acc / 8
    let unary = -acc
    let neg = !flag
    return acc + unary + (if (neg) { 1 } else { 0 }) + cell
}

func valuesOf(): Array<Int64> {
    return [1, 2, 3]
}

func longConditionChain(a: Int64, b: Int64, c: Int64, d: Int64, e: Int64, f: Int64): Bool {
    return a > 0 && b > 1 || c < 2 && d <= 3 || e != 4 && f >= 5 || !(a == b) || a << 1 > 8
}
