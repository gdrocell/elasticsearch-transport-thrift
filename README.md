Thrift Transport for Elasticsearch
==================================

The thrift transport plugin allows to use the REST interface over [thrift](http://thrift.apache.org/) on top of HTTP.

<<<<<<< HEAD
In order to install the plugin, simply run: 

```sh
bin/plugin -install elasticsearch/elasticsearch-transport-thrift/2.4.0
```

You need to install a version matching your Elasticsearch version:

| elasticsearch |    Thrift Plugin      |   Docs     |  
|---------------|-----------------------|------------|
| master        |  Build from source    | See below  |
| es-1.x        |  Build from source    | [2.5.0-SNAPSHOT](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/es-1.x/#version-250-snapshot-for-elasticsearch-1x)  |
|    es-1.4              |     2.4.0         | [2.4.0](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/v2.4.0/#version-240-for-elasticsearch-14)                  |
| es-1.3        |  2.3.0                | [2.3.0](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/v2.3.0/#thrift-transport-for-elasticsearch)  |
| es-1.2        |  2.2.0                | [2.2.0](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/v2.2.0/#thrift-transport-for-elasticsearch)  |
| es-1.0        |  2.0.0                | [2.0.0](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/v2.0.0/#thrift-transport-for-elasticsearch)  |
| es-0.90       |  1.8.0                | [1.8.0](https://github.com/elasticsearch/elasticsearch-transport-thrift/tree/v1.8.0/#thrift-transport-for-elasticsearch)  |

To build a `SNAPSHOT` version, you need to build it with Maven:

```bash
mvn clean install
plugin --install transport-thrift \
       --url file:target/releases/elasticsearch-transport-thrift-X.X.X-SNAPSHOT.zip
```

=======
## Version 2.5.1-SNAPSHOT for Elasticsearch: 1.5

If you are looking for another version documentation, please refer to the 
[compatibility matrix](http://github.com/elasticsearch/elasticsearch-transport-thrift#thrift-transport-for-elasticsearch).

>>>>>>> upstream/es-1.5
## Guide

The thrift definition can be found under the `elasticsearch.thrift` file.

The thrift [schema](https://github.com/elasticsearch/elasticsearch-transport-thrift/blob/master/elasticsearch.thrift) can be used to generate thrift clients.

* `thrift.port`: The port to bind to. Defaults to `9500-9600`.
* `thrift.frame`: Defaults to `-1`, which means no framing. Set to a higher value to specify the frame size (like `15mb`).
* `thrift.bind_host`: Set explicit bindings for thrift protocol. Defaults to `transport.bind_host` or `transport.host`.
* `thrift.publish_host`: Set explicit bindings for thrift protocol. Defaults to `transport.publish_host` or `transport.host`.
* `thrift.protocol`: `binary` (default) which use Binary protocol or `compact` which uses Compact Protocol. See [Thrift documentation](https://thrift.apache.org/docs/concepts).

## Ezbake Configuration

You need to make sure that you set the EZCONFIGURATION_DIR variable, once the plugin is installed.

```bash
	export EZCONFIGURATION_DIR=/path/to/config
```


License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2014 Elasticsearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
