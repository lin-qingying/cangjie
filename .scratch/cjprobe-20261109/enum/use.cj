func ofCode(value: UInt8): ResponseCode {
    match (value) {
        case 0x00 => Ok
        case 0x01 => AuthMoreData
        case 0xFF => Error
        case _ => Ok
    }
}

func ofColumn(value: UInt8): ColumnType {
    match (value) {
        case 0x01 => Tiny
        case 0xFE => String
        case _ => Tiny
    }
}
