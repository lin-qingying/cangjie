<?xml version="1.0"?>
<command>
    <#if etsWrapperAction == "YES">
        <#include "ets/command.xml.ftl" />
        <#include "cangjie/command.xml.ftl" />
    <#else>
        <#include "cangjie/command.xml.ftl" />
    </#if>
</command>
