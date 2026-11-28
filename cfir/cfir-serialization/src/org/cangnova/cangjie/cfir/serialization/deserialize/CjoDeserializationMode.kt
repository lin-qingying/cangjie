package org.cangnova.cangjie.cfir.serialization.deserialize

/** 同一模块的两种读取视图分别拥有声明与类型缓存；签名读取不触发 initializer 物化。 */
enum class CjoDeserializationMode { FULL, SIGNATURE_ONLY }
