/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.thrift;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.transport.BindTransportException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import ezbake.common.properties.EzProperties;
import ezbake.configuration.ClasspathConfigurationLoader;
import ezbake.configuration.DirectoryConfigurationLoader;
import ezbake.configuration.EzConfiguration;
import ezbake.thrift.ThriftUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.file.Path;
import java.nio.file.Paths;


import static org.elasticsearch.common.util.concurrent.EsExecutors.daemonThreadFactory;

/**
 * Modified by Gary Drocella 10/22/14
 */
public class ThriftServer extends AbstractLifecycleComponent<ThriftServer> {

    final int frame;

    final String port;

    final String bindHost;

    final String publishHost;

    private final NetworkService networkService;

    private final NodeService nodeService;

    private final ThriftRestImpl client;

    private final TProtocolFactory protocolFactory;

    private volatile TServer server;

    private volatile int portNumber;

    private final String USE_SSL_KEY = "thrift.use.ssl";
    
    private final ESLogger logger = Loggers.getLogger(ThriftServer.class);
    
    @Inject
    public ThriftServer(Settings settings, NetworkService networkService, NodeService nodeService, ThriftRestImpl client) {
        super(settings);
        this.client = client;
        this.networkService = networkService;
        this.nodeService = nodeService;
        this.frame = (int) componentSettings.getAsBytesSize("frame", new ByteSizeValue(-1)).bytes();
        this.port = componentSettings.get("port", "9500-9600");
        this.bindHost = componentSettings.get("bind_host", settings.get("transport.bind_host", settings.get("transport.host")));
        this.publishHost = componentSettings.get("publish_host", settings.get("transport.publish_host", settings.get("transport.host")));

        logger.debug("Using the protocol {}", componentSettings.get("protocol", "binary"));
        
        if (componentSettings.get("protocol", "binary").equals("compact")) {
            protocolFactory = new TCompactProtocol.Factory();
        } else {
            protocolFactory = new TBinaryProtocol.Factory();
        }
        
        logger.info("ssl directory is ", settings.get("ezbake.security.ssl.dir"));
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        InetAddress bindAddrX;
        try {
            bindAddrX = networkService.resolveBindHostAddress(bindHost);
        } catch (IOException e) {
            throw new BindTransportException("Failed to resolve host [" + bindHost + "]", e);
        }
        final InetAddress bindAddr = bindAddrX;

        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<Exception>();
        boolean success = portsRange.iterate(new PortsRange.PortCallback() {
            @Override
            public boolean onPortNumber(int portNumber) {
                ThriftServer.this.portNumber = portNumber;
               
                try {
                    EzConfiguration ezconfig = new EzConfiguration();
                    Properties props = new DirectoryConfigurationLoader().loadConfiguration();
                    
                    mergeProperties(props, settings);
                    
                    boolean useSsl = settings.getAsBoolean("thrift.use.ssl", false);
                    Rest.Processor processor = new Rest.Processor(client);

                    // Bind and start to accept incoming connections.
                    // Inserting code to use ssl in thrift transport
                    
                    TServerSocket serverSocket = null;
                    
                    if(!useSsl) {
                        serverSocket = new TServerSocket(new InetSocketAddress(bindAddr, portNumber));
                    }
                    else {
                        logger.info("Using SSL Server Socket");
                        serverSocket = ThriftUtils.getSslServerSocket(new InetSocketAddress(bindAddr, portNumber), props);
                    }

                    TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverSocket)
                            .minWorkerThreads(16)
                            .maxWorkerThreads(Integer.MAX_VALUE)
                            .inputProtocolFactory(protocolFactory)
                            .outputProtocolFactory(protocolFactory)
                            .processor(processor);
                    
                    if (frame <= 0) {
                        args.inputTransportFactory(new TTransportFactory());
                        args.outputTransportFactory(new TTransportFactory());
                    } else {
                        args.inputTransportFactory(new TFramedTransport.Factory(frame));
                        args.outputTransportFactory(new TFramedTransport.Factory(frame));
                    }

                    server = new TThreadPoolServer(args);
                } catch (Exception e) {
                    lastException.set(e);
                    return false;
                }
                return true;
            }
        });
        if (!success) {
            throw new BindTransportException("Failed to bind to [" + port + "]", lastException.get());
        }
        logger.info("bound on port [{}]", portNumber);
        try {
            nodeService.putAttribute("thrift_address", new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost), portNumber).toString());
        } catch (Exception e) {
            // ignore
        }

        daemonThreadFactory(settings, "thrift_server").newThread(new Runnable() {
            @Override
            public void run() {
                logger.info("Server Serving");
                server.serve();
            }
        }).start();
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        nodeService.removeAttribute("thrift_address");
        server.stop();
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }
    
    private void mergeProperties(Properties props, Settings settings) {
        props.setProperty("ezbake.security.ssl.dir", settings.get("ezbake.security.ssl.dir"));
        props.setProperty("ezbake.security.app.id", settings.get("ezbake.security.app.id"));
        props.setProperty("thrift.use.ssl", settings.get("thrift.use.ssl"));
    }
}
