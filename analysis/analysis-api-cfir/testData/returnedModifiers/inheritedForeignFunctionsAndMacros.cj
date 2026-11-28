package returned.modifiers

foreign {
    func nativeRead(): Int64
    macro nativeMacro(): Unit {}
}

func ordinary(): Unit {}
