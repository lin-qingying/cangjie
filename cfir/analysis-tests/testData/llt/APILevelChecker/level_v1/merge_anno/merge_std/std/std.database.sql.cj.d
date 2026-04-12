// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package std.database.sql

import std.collection.Map
import std.collection.HashMap
import std.sync.ReentrantMutex
import std.sort.stableSort
import std.random.Random
import std.time.DateTime
import std.sync.AtomicBool
import std.collection.{
    Map,
    ArrayList
}
import std.collection.concurrent.{
    BlockingQueue,
    LinkedBlockingQueue
}
import std.sync.{
    Timer,
    CatchupStyle,
    AtomicBool,
    ReentrantMutex
}
import std.io.InputStream
import std.math.numeric.Decimal

/**
* ColumnInfo, contain the name, type, length of a column.
*/
@!APILevel[12, atomicservice : true]
public interface ColumnInfo {
}

/**
* This Connection interface executes SQL statements and processes transactions.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface Connection <: Resource {
}

@!APILevel[12, atomicservice : true]
public enum ConnectionState <: Equatable<ConnectionState> {
    @!APILevel[12, atomicservice : true]
    /**
    * The connection to the data source is broken.
    * This can occur only after the connection has been opened.
    * A connection in this state may be closed and then re-opened.
    *
    * @since 0.40.1
    */
    Broken |
    @!APILevel[12, atomicservice : true]
    /**
    * The connection is closed.
    *
    * @since 0.40.1
    */
    Closed |
    @!APILevel[12, atomicservice : true]
    /**
    * The connection object is connecting to the data source.
    *
    * @since 0.40.1
    */
    Connecting |
    @!APILevel[12, atomicservice : true]
    /**
    * The connection is connected.
    *
    * @since 0.40.1
    */
    Connected
    @!APILevel[12, atomicservice : true]
    public operator func ==(rhs: ConnectionState): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(rhs: ConnectionState): Bool
}

/**
* This Datasource interface is used to process the connection to the database.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface Datasource <: Resource {
}

/**
* The Driver interface is used to obtain datasource instance objects.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface Driver {
}

/**
* Load an Cangjie database driver based on name.
*
* @since 0.40.1
*/
@!APILevel[12, atomicservice : true]
public class DriverManager {
    /**
    * makes a database driver available by the provided name. This method is thread safe.
    * SqlException - the driverName already registered.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public static func register(driverName: String, driver: Driver): Unit
    
    /**
    * Unregister the database driver by name. This method is thread safe.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public static func deregister(driverName: String): Unit
    
    /**
    * This command is used to obtain the registered database driver by name.
    * If the registered database driver does not exist, None is returned. This method is thread safe.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public static func getDriver(driverName: String): Option<Driver>
    
    /**
    * Returns a list of registered database driver names (sorted). This method is thread safe.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public static func drivers(): Array<String>
}

/**
* connection pooled datasource implementation
*/
@!APILevel[12, atomicservice : true]
public class PooledDatasource <: Datasource {
    /**
    * Initialize a PooledDatasource with specific datasource.
    *
    * @param datasource implement by driver
    *
    * @since 0.20.4
    */
    @!APILevel[12, atomicservice : true]
    public init(datasource: Datasource)
    
    /**
    * Maximum duration for which connections are allowed to remain idle in the pool. Idle connections that exceed the duration may be reclaimed.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop idleTimeout: Duration
    
    /**
    * Duration since the connection was created, after which the connection is automatically closed.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop maxLifeTime: Duration
    
    /**
    * Interval for checking the health of an idle connection to prevent it from being timed out by the database or network infrastructure.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop keepaliveTime: Duration
    
    /**
    * Maximum number of connections in the connection pool. If the value is less than or equal to 0, the value is Int32.Max.
    * If the value is smaller than the current value, the setting does not take effect immediately. The setting takes effect only after idle connections are reclaimed and closed.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop maxSize: Int32
    
    /**
    * Maximum number of idle connections. If the number of idle connections exceeds the value of this parameter, the idle connections will be closed. If the value is less than or equal to 0, the value is Int32.Max.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop maxIdleSize: Int32
    
    /**
    * Timeout interval for obtaining a connection from the pool. If the timeout interval expires, the system throws SqlException.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public mut prop connectionTimeout: Duration
    
    /**
    * setting database driver connection options, common keys are predefined in SqlOption.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public func setOption(key: String, value: String): Unit
    
    /**
    * Returns an available connection.
    *
    * @return a datasource connection instance.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public func connect(): Connection
    
    /**
    * Reports whether the connection is closed.
    *
    * @return true if closed, otherwise false.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public func isClosed(): Bool
    
    /**
    * closes all connections in the pool and rejects future connect calls. Blocks until all connections are returned to pool and closed.
    *
    * @since 0.40.1
    */
    @!APILevel[12, atomicservice : true]
    public func close(): Unit
}

/**
* The QueryResult interface is used to represent the result set of a query statement.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface QueryResult <: Resource {
}

/**
* SqlDbType is the base class of all Sql data types. To extend user-defined types, inherit SqlDbType or SqlNullableDbType.
* All implementation types must have a public value property.
*
* @since 0.40.1
*/
@Deprecated
@!APILevel[12, atomicservice : true]
public interface SqlDbType {
}

/**
* Base class of the type that allows null values. If the value is null, the value field value is Option.None.
*
* @since 0.40.1
*/
@Deprecated
@!APILevel[12, atomicservice : true]
public interface SqlNullableDbType <: SqlDbType {
}

/**
* Fixed-length character string, corresponding to the Cangjie String type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `String` instead."]
@!APILevel[12, atomicservice : true]
public class SqlChar <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: String)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: String
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Fixed-length character string, corresponding to the Cangjie String type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?String` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableChar <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?String)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?String
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length character string, corresponding to the Cangjie String type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `String` instead."]
@!APILevel[12, atomicservice : true]
public class SqlVarchar <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: String)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: String
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length character string, corresponding to the Cangjie String type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?String` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableVarchar <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?String)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?String
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Fixed-length binary string, corresponding to the Cangjie Array<Byte> type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Array<Byte>` instead."]
@!APILevel[12, atomicservice : true]
public class SqlBinary <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Array<Byte>)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Array<Byte>
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Fixed-length binary character string corresponding to the Array<Byte> type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Array<Byte>` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableBinary <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Array<Byte>)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Array<Byte>
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length binary character string, corresponding to the Cangjie Array<Byte> type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Array<Byte>` instead."]
@!APILevel[12, atomicservice : true]
public class SqlVarBinary <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Array<Byte>)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Array<Byte>
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length binary character string, corresponding to the Array<Byte> type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Array<Byte>` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableVarBinary <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Array<Byte>)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Array<Byte>
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length character data (character large object), corresponding to the Cangjie InputStream type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `InputStream` instead."]
@!APILevel[12, atomicservice : true]
public class SqlClob <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: InputStream)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: InputStream
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length character data (character large object), corresponding to the Cangjie InputStream type, which can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?InputStream` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableClob <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?InputStream)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?InputStream
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length binary character data (Binary Large Object), corresponding to the Cangjie InputStream type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `InputStream` instead."]
@!APILevel[12, atomicservice : true]
public class SqlBlob <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: InputStream)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: InputStream
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Variable-length binary character data (Binary Large Object), corresponding to the Cangjie InputStream type, which can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?InputStream` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableBlob <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?InputStream)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?InputStream
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Boolean type, corresponding to the Cangjie Bool type
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Bool` instead."]
@!APILevel[12, atomicservice : true]
public class SqlBool <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Bool)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Bool
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Boolean type, corresponding to the warehouse Bool type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Bool` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableBool <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Bool)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Bool
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Byte, corresponding to the Cangjie Int8 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Int8` instead."]
@!APILevel[12, atomicservice : true]
public class SqlByte <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Int8)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Int8
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Byte, corresponding to the warehouse Int8 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Int8` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableByte <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Int8)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Int8
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Small integer, corresponding to the Cangjie Int16 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Int16` instead."]
@!APILevel[12, atomicservice : true]
public class SqlSmallInt <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Int16)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Int16
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Small integer, corresponding to the Cangjie Int16 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Int16` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableSmallInt <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Int16)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Int16
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Medium integer, corresponding to Cangjie Int32 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Int32` instead."]
@!APILevel[12, atomicservice : true]
public class SqlInteger <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Int32)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Int32
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Medium integer, corresponding to Cangjie Int32 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Int32` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableInteger <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Int32)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Int32
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Large integer, corresponding to the Cangjie Int64 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Int64` instead."]
@!APILevel[12, atomicservice : true]
public class SqlBigInt <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Int64)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Large integer, corresponding to the Cangjie Int64 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Int64` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableBigInt <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Int64)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Int64
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Floating point number, corresponding to the Cangjie Float32 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Float32` instead."]
@!APILevel[12, atomicservice : true]
public class SqlReal <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Float32)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Float32
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Floating point number, corresponding to the Cangjie Float32 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Float32` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableReal <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Float32)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Float32
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Double-precision number, corresponding to the Cangjie Float64 type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Float64` instead."]
@!APILevel[12, atomicservice : true]
public class SqlDouble <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Float64)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Float64
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Double-precision number, corresponding to the Cangjie Float64 type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Float64` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableDouble <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Float64)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Float64
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Date, which is valid only for the year, month, and day. It corresponds to the Cangjie DateTime type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlDate <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Date, which is valid only for the year, month, and day. It corresponds to the Cangjie DateTime type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableDate <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Time, which is valid only for hour, minute, second, and millisecond. It corresponds to the Cangjie DateTime type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlTime <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Time, which is valid only for hour, minute, second, and millisecond. It corresponds to the Cangjie DateTime type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableTime <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Time with a time zone, which is valid only for the hour, minute, second, and millisecond and timezone. It corresponds to the Cangjie DateTime type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlTimeTz <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Time with a time zone, which is valid only for the hour, minute, second, and millisecond and timezone. It corresponds to the Cangjie DateTime type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableTimeTz <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Timestamp, corresponding to the Cangjie DateTime type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlTimestamp <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Timestamp, corresponding to the Cangjie DateTime type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?DateTime` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableTimestamp <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?DateTime)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?DateTime
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Interval, corresponding to the Cangjie Duration type.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `Duration` instead."]
@!APILevel[12, atomicservice : true]
public class SqlInterval <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Duration)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Duration
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* Interval, corresponding to the Cangjie Duration type. The value can be null.
*
* @since 0.40.1
*/
@Deprecated[message: "Use `?Duration` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableInterval <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Duration)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Duration
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* SqlDecimal, corresponding to the Cangjie Decimal type.
*
* @since 0.50.2
*/
@Deprecated[message: "Use `Decimal` instead."]
@!APILevel[12, atomicservice : true]
public class SqlDecimal <: SqlDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: Decimal)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: Decimal
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* SqlNullableDecimal, corresponding to the Cangjie Decimal type. The value can be null.
*
* @since 0.50.2
*/
@Deprecated[message: "Use `?Decimal` instead."]
@!APILevel[12, atomicservice : true]
public class SqlNullableDecimal <: SqlNullableDbType {
    @!APILevel[12, atomicservice : true]
    public init(v: ?Decimal)
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?Decimal
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

/**
* The SqlException class is used to handle SQL-related exceptions.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public open class SqlException <: Exception {
    /**
    * a five-character string where IDMS returns the status of the last SQL statement executed.
    *
    * @since 0.43.2
    */
    @!APILevel[12, atomicservice : true]
    public prop sqlState: String
    
    /**
    * error code that is specific to each vendor.
    *
    * @since 0.43.2
    */
    @!APILevel[12, atomicservice : true]
    public prop errorCode: Int64
    
    @!APILevel[12, atomicservice : true]
    public override prop message: String
    
    /**
    * Parameterless constructor.
    *
    * @since 0.32.3
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Parameter constructor.
    *
    * @param message : predefined message.
    * @param sqlState : a five-character string where IDMS returns the status of the last SQL statement executed.
    * @param errorCode : an integer error code that is specific to each vendor.
    *
    * @since 0.43.2
    */
    @!APILevel[12, atomicservice : true]
    public init(message: String, sqlState: String, errorCode: Int64)
    
    /**
    * Parameter constructor.
    *
    * @param message : predefined message.
    *
    * @since 0.32.3
    */
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* Predefined SQL option name and value. If extension is required, do not conflict with these names and values.
*/
@!APILevel[12, atomicservice : true]
public class SqlOption {
    /**
    * URL, which is usually used for database connection strings in SQL api.
    */
    @!APILevel[12, atomicservice : true]
    public static const URL = "url"
    
    /**
    * Host, host name or IP address of the database server
    */
    @!APILevel[12, atomicservice : true]
    public static const Host = "host"
    
    /**
    * Username, user name for connecting to the database
    */
    @!APILevel[12, atomicservice : true]
    public static const Username = "username"
    
    /**
    * Password, password for connecting to the database
    */
    @!APILevel[12, atomicservice : true]
    public static const Password = "password"
    
    /**
    * Driver, database driver name, for example, postgres and opengauss.
    */
    @!APILevel[12, atomicservice : true]
    public static const Driver = "driver"
    
    /**
    * Database, database Name
    */
    @!APILevel[12, atomicservice : true]
    public static const Database = "database"
    
    /**
    * Encoding, encoding type of the database character set.
    */
    @!APILevel[12, atomicservice : true]
    public static const Encoding = "encoding"
    
    /**
    * ConnectionTimeout, timeout interval of the connect operation, in milliseconds.
    */
    @!APILevel[12, atomicservice : true]
    public static const ConnectionTimeout = "connection_timeout"
    
    /**
    * UpdateTimeout, timeout interval of the update operation, in milliseconds.
    */
    @!APILevel[12, atomicservice : true]
    public static const UpdateTimeout = "update_timeout"
    
    /**
    * QueryTimeout, Timeout interval of the query operation, in milliseconds.
    */
    @!APILevel[12, atomicservice : true]
    public static const QueryTimeout = "query_timeout"
    
    /**
    * FetchRows, Specifies the number of rows to fetch from the database when additional rows need to be fetched
    */
    @!APILevel[12, atomicservice : true]
    public static const FetchRows = "fetch_rows"
    
    /**
    * SSLMode, transport layer encryption mode
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLMode = "ssl.mode"
    
    /**
    * SSLModePreferred, value for SSLMode, first try an SSL connection; if that fails, try a non-SSL connection.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLModePreferred = "ssl.mode.preferred"
    
    /**
    * SSLModeDisabled, value for SSLMode, Establish an unencrypted connection.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLModeDisabled = "ssl.mode.disabled"
    
    /**
    * SSLModeRequired, value for SSLMode, only try an SSL connection. If a root CA file is present, verify the certificate in the same way as if verify-ca was specified.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLModeRequired = "ssl.mode.required"
    
    /**
    * SSLModeVerifyCA, value for SSLMode, only try an SSL connection, and verify that the server certificate is issued by a trusted certificate authority (CA).
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLModeVerifyCA = "ssl.mode.verify_ca"
    
    /**
    * SSLModeVerifyFull, value for SSLMode, only try an SSL connection, verify that the server certificate is issued by a trusted CA and that the requested server host name matches that in the certificate.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLModeVerifyFull = "ssl.mode.verify_full"
    
    /**
    * SSLCA, specifies the name of a file containing SSL certificate authority (CA) certificate(s).
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLCA = "ssl.ca"
    
    /**
    * SSLCert, specifies the file name of the client SSL certificate.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLCert = "ssl.cert"
    
    /**
    * SSLKey, specifies the location for the secret key used for the client certificate.
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLKey = "ssl.key"
    
    /**
    * SSLKeyPassword, the password for the secret key specified in SSLKey
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLKeyPassword = "ssl.key.password"
    
    /**
    * SSLSni, setting the value "Server Name Indication" (SNI) on SSL-enabled connections, in Bool type
    */
    @!APILevel[12, atomicservice : true]
    public static const SSLSni = "ssl.sni"
    
    /**
    * Tls12Ciphersuites, The list of permissible encryption ciphers for connections that use TLS protocols up through TLSv1.2.
    * The value is a list of zero or more colon-separated ciphersuite names.
    * For example: "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
    */
    @!APILevel[12, atomicservice : true]
    public static const Tls12Ciphersuites = "tls1.2.ciphersuites"
    
    /**
    * Tls13Ciphersuites, specifies which ciphersuites the client permits for encrypted connections that use TLSv1.3.
    * The value is a list of zero or more colon-separated ciphersuite names.
    * For example: "TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256"
    */
    @!APILevel[12, atomicservice : true]
    public static const Tls13Ciphersuites = "tls1.3.ciphersuites"
    
    /**
    * TlsVersion, specifies the TLS protocols the client permits for encrypted connections.
    * The value is a list of one or more comma-separated protocol versions.
    * For example:"TLSv1.2,TLSv1.3"
    */
    @!APILevel[12, atomicservice : true]
    public static const TlsVersion = "tls.version"
}

/**
* The Statement interface is bound to a Connection, that is the pre-execution interface of SQL statements.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface Statement <: Resource {
}

/**
* Transaction isolation defines when and how the results of an operation in a transaction are visible to other concurrent transaction operations in a database system.
*/
@!APILevel[12, atomicservice : true]
public enum TransactionIsoLevel <: ToString & Hashable & Equatable<TransactionIsoLevel> {
    @!APILevel[12, atomicservice : true]
    // unspecified transaction isolation level, the behavior depends on a specific database server.
    Unspecified |
    @!APILevel[12, atomicservice : true]
    // The transaction waits until rows write-locked by other transactions are unlocked; this prevents it from reading any "dirty" data.
    ReadCommitted |
    @!APILevel[12, atomicservice : true]
    // Transactions are not isolated from each other. 
    ReadUncommitted |
    @!APILevel[12, atomicservice : true]
    // The transaction waits until rows write-locked by other transactions are unlocked; this prevents it from reading any "dirty" data.
    RepeatableRead |
    @!APILevel[12, atomicservice : true]
    // Snapshot isolation avoids most locking and blocking by using row versioning. 
    Snapshot |
    @!APILevel[12, atomicservice : true]
    // The transaction waits until rows write-locked by other transactions are unlocked; this prevents it from reading any "dirty" data.
    Serializable |
    @!APILevel[12, atomicservice : true]
    // Linearizability is relevant when you are looking at a subset of operations on a single object (i.e. a db row or a nosql record).
    Linearizable |
    @!APILevel[12, atomicservice : true]
    // The pending changes from more highly isolated transactions cannot be overwritten.
    Chaos
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(rhs: TransactionIsoLevel): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(rhs: TransactionIsoLevel): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

/**
* Transactional read/write mode.
*/
@!APILevel[12, atomicservice : true]
public enum TransactionAccessMode <: ToString & Hashable & Equatable<TransactionAccessMode> {
    @!APILevel[12, atomicservice : true]
    // unspecified transaction access mode, the behavior depends on a specific database server.
    Unspecified |
    @!APILevel[12, atomicservice : true]
    // read-write mode
    ReadWrite |
    @!APILevel[12, atomicservice : true]
    // readonly mode
    ReadOnly
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(rhs: TransactionAccessMode): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(rhs: TransactionAccessMode): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

/**
* Deferred Mode for Transactions.
*/
@!APILevel[12, atomicservice : true]
public enum TransactionDeferrableMode <: ToString & Hashable & Equatable<TransactionDeferrableMode> {
    @!APILevel[12, atomicservice : true]
    // Unspecified transaction defer mode, the behavior depends on a specific database server.
    Unspecified |
    @!APILevel[12, atomicservice : true]
    // DEFERRABLE
    Deferrable |
    @!APILevel[12, atomicservice : true]
    // NOT_DEFERRABLE
    NotDeferrable
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(rhs: TransactionDeferrableMode): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(rhs: TransactionDeferrableMode): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

/**
* Defines the core behavior of database transactions.
*/
@!APILevel[12, atomicservice : true]
public interface Transaction {
}

/**
* The UpdateResult interface indicates the result of executing the Insert, Update, and Delete statements.
*
* @since 0.32.3
*/
@!APILevel[12, atomicservice : true]
public interface UpdateResult {
}

