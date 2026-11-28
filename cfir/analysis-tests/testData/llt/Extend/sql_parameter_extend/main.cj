// FILE: mariadb/connection/connection.cj


package mariadb.connection

import std.io.*

public const DB_DATE_TIME_FORMAT: String = "yyyy-MM-dd HH:mm:ss"

public func escapeString(value: String): String {
    return "'" + value + "'"
}

public func toHexStringWithPrefix(buffer: ByteBuffer): String {
    return "0x"
}

public func toHexStringWithPrefix(values: Array<Byte>): String {
    return "0x"
}

// FILE: main.cj


import std.io.*
import std.time.*
import std.math.numeric.*
import mariadb.connection.*

public interface SqlParameter {

    func asParameter(): String

}

extend ?Any <: SqlParameter {

    public func asParameter(): String {
        if (this.isNone()) {
            return "NULL"
        } else {
            return ""
        }
    }

}

extend Bool <: SqlParameter {

    public func asParameter(): String {
        if (this) {
            return "1"
        }else {
            return "0"
        }
    }

}

extend Int8 <: SqlParameter {

    public func asParameter(): String {
        return this.toString()
    }

}

extend UInt8 <: SqlParameter {

    public func asParameter(): String {
        return this.toString()
    }

}

extend Int16 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend UInt16 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend Int32 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend UInt32 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend Int64 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend UInt64 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend Float16 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend Float32 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend Float64 <: SqlParameter {
    public func asParameter(): String {
        return this.toString()
    }
}

extend ByteBuffer <: SqlParameter {

    public func asParameter(): String {
        return toHexStringWithPrefix(this)
    }
    
}

extend Array<Byte> <: SqlParameter {

    public func asParameter(): String {
        return toHexStringWithPrefix(this)
    }

}

extend String <: SqlParameter {

    public func asParameter(): String {
        return escapeString(this)
    }

}

extend DateTime <: SqlParameter {

    public func asParameter(): String {
        return "'" + this.format(DB_DATE_TIME_FORMAT) + "'"
    }

}

extend BigInt <: SqlParameter {

    public func asParameter(): String {
        return "'" + this.toString() + "'"
    }

}

extend Decimal <: SqlParameter {

    public func asParameter(): String {
        return "'" + this.toString() + "'"
    }

}
