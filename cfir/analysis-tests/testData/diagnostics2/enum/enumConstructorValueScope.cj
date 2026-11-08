// FILE: enumCtorScopeDecl.cj
protected package mariadbproto

// 形态照 mariadb-driver（`protected package` + `protected enum` + 枚举自带成员函数）：
// enum 与使用点分处两个文件、同一包，裸枚举构造器名出现在
// `super(...)` 实参、`if` 条件里的成员调用实参、返回值等值位置。
protected enum CommandType {
    | Sleep
    | Query
    | StmtPrepare
    | Quit

    internal func value(): UInt8 {
        match (this) {
            case Query => 3
            case StmtPrepare => 22
            case _ => 0
        }
    }
}

protected enum Capability {
    | LongPassword
    | SSL
    | ConnectWithDB
    | PluginAuth

    internal func value(): UInt32 {
        match (this) {
            case LongPassword => 0x00000001
            case SSL => 0x00000800
            case ConnectWithDB => 0x00000008
            case PluginAuth => 0x00080000
        }
    }
}

protected abstract class Request {
    protected let number: UInt8

    protected init(number: UInt8) {
        this.number = number
    }
}

protected abstract class Command <: Request {
    protected let commandType: CommandType

    protected init(commandType: CommandType) {
        super(0)
        this.commandType = commandType
    }
}

// FILE: enumCtorScopeUse.cj
protected package mariadbproto

protected class QueryCommand <: Command {
    protected init(query: String) {
        super(Query)
        let _ = query
    }
}

protected class HandshakeResponse <: Request {
    private var capabilities: UInt32 = 0x00

    protected init() {
        super(1)
    }

    protected func hasCapability(capability: Capability): Bool {
        return (capabilities & capability.value()) == capability.value()
    }

    func check(): Bool {
        if (hasCapability(SSL)) {
            return true
        }
        if (hasCapability(ConnectWithDB)) {
            return true
        }
        return false
    }
}

func defaultCommand(): CommandType {
    return Quit
}
