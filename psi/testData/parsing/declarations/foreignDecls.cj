// foreign 声明：单条 foreign func、foreign 块与 unsafe 调用块。
// 官方依据：Cangjie 文档《C 互操作》《foreign》；cjc 1.0.5 / 1.1.3 零诊断。
foreign func puts(s: CString): Int32

foreign func getpid(): Int32

foreign {
    func strlen(s: CString): Int64
    func memcpy(dst: CPointer<UInt8>, src: CPointer<UInt8>, n: Int64): Unit
}

class Native {
    private var fd: Int32 = 0

    public func read(): Int32 {
        unsafe {
            return getpid()
        }
    }

    public func write(s: CString): Int32 {
        unsafe {
            return puts(s)
        }
    }
}
