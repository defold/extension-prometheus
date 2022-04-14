# extension-prometheus
Lua obfuscation using [Prometheus](https://github.com/levno-710/Prometheus). This extension hooks into the Lua builder plugin system in bob.


## Prometheus config
The extension is running Prometheus with the following configuration:

```
lua cli.lua --preset Weak --out out.lua in.lua 
```


## Prometheus source code
Prometheus source code is packaged into `pluginPrometheusSource.jar` and unpacked using `getResourceAsStream()` to a temporary folder while bob is running.




