# extension-prometheus
Lua obfuscation using [Prometheus](https://github.com/levno-710/Prometheus). This extension hooks into the [Lua builder plugin system in bob](https://github.com/defold/defold/blob/dev/com.dynamo.cr/com.dynamo.cr.bob/src/com/dynamo/bob/pipeline/LuaBuilder.java#L243-L252).

## Installation
To use this library in your Defold project you need to [make a fork of this repository](https://github.com/defold/extension-prometheus/fork) and then add the repository archive URL to your `game.project` dependencies. Example (replace `%YOUR_ORG%` with your GitHub user or organisation):

https://github.com/%YOUR_ORG%/extension-prometheus/archive/master.zip

## Prometheus config
The extension will look for a [Prometheus configuration](https://levno-710.gitbook.io/prometheus/getting-started/writing-a-custom-config-file) file named `prometheus.lua` in the project root.

Prometheus can be completely disabled by setting the `prometheus.disabled` **game.project** option:

```
[prometheus]
disabled = 1
```

## Example

```lua
local test = require("main.test")

function init(self)
 print("hello")
 test.greet("Bob")
end
```

```lua
return(function(...)local W={"LGVa";"1OD91aRxhfwx";"dc2A1cU=","ho04hfL="}local function a(E)return W[E+50784]end for E,a in ipairs({{1,4},{1;3},{4,4}})do while a[1]<a[2]do W[a[1]],W[a[2]],a[1],a[2]=W[a[2]],W[a[1]],a[1]+1,a[2]-1 end end do local E=type local a=W local Y=string.sub local x=table.insert local f=string.len local j={["7"]=1;h=25,C=56,K=17,L=16;["+"]=47,v=7,a=34;["0"]=9;q=51;["8"]=40,d=26,H=50;["3"]=3;r=4,w=13,Y=63,M=30,W=36,R=57,U=60,["6"]=31;x=52;Q=14,S=45,f=23;b=24,e=8;Z=29;z=46;i=43;t=12,F=2;g=28;E=20,["9"]=41,G=38;n=62,j=49;N=54;["/"]=33;p=59;X=0,P=53,["2"]=21,O=22;y=11;c=6,I=58;o=55,A=44;["1"]=27,k=10;J=18;D=5;["5"]=32;V=61,["4"]=37;s=35,m=48,B=39,T=15,l=19,u=42}local D=table.concat local F=math.floor local o=string.char for W=1,#a,1 do local R=a[W]if E(R)=="string"then local E=f(R)local g={}local P=1 local l=0 local M=0 while P<=E do local W=Y(R,P,P)local a=j[W]if a then l=l+a*64^(3-M)M=M+1 if M==4 then M=0 local E=F(l/65536)local W=F((l%65536)/256)local a=l%256 x(g,o(E,W,a))l=0 end elseif W=="="then x(g,o(F(l/65536)))if P>=E or Y(R,P+1,P+1)~="="then x(g,o(F((l%65536)/256)))end break end P=P+1 end a[W]=D(g)end end end local E=require(a(-50783))function init(W)print(a(-50782))E[a(-50781)](a(-50780))end end)(...)
```

## Prometheus source code
Prometheus source code is packaged into `pluginPrometheusSource.jar` and unpacked using `getResourceAsStream()` to a temporary folder while bob is running.