package completion.expectedparametertype

class Alpha {}
class Beta {}

// 仓颉的命名参数在**声明侧**用 `!` 标注，调用侧才能写 `name: value`。
func takes(other!: Beta, value!: Alpha): Alpha {
    value
}

func namedArgumentBindsByParameterName() {
    takes(other: Beta(), value: Alpha())
}
