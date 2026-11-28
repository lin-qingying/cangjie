package source_declarations_calls

// 普通 private 函数由默认正文引用；应导出声明身份，正文保持 body=0。
private func privateStep(value: Int64): Int64 {
    value + 1
}

public class SourceReceiver {
    public func add(value: Int64): Int64 {
        value + 10
    }
}

// 接口有正文的方法具有原生 DEFAULT 语义；不需要自造导出注解。
public interface DeclarationsCalls {
    func evaluate(receiver: SourceReceiver, input: Int64): Int64 {
        let step: (Int64) -> Int64 = privateStep
        var local = input
        func nested(value: Int64): Int64 {
            value + input
        }
        let transform: (Int64) -> Int64 = { value: Int64 => nested(value) }
        local = step(local)
        return receiver.add(transform(local))
    }

    func implicitValue(input: Int64): Int64 {
        let copy = input
        copy
    }

    func explicitUnit(): Unit {
        return
    }
}
