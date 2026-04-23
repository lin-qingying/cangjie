<?xml version="1.0"?>
<command>
    <#if enableCangjieAction == "YES">
        <#include "cangjie/command.xml.ftl" />
    <#else>
        <#include "ets/command.xml.ftl" />
        <#include "cangjie/command.xml.ftl" />
    </#if>
</command>
