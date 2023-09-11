/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.modelproviders.YamlProvider
import kotlin.io.path.Path

object ConfigurationProvider {
    fun getEtcdEndpoints(configurationPath: String): List<String> {
        val distributionConfiguration = getConfiguration(configurationPath)
        val etcdConfiguration = distributionConfiguration["etcd"]
        requireNotNull(etcdConfiguration) {
            "Configuration for etcd not found"
        }
        require(etcdConfiguration is Map<*, *>) {
            "Configuration for etcd must be a map"
        }
        println(etcdConfiguration)
        val endpoints = etcdConfiguration["endpoints"]
        requireNotNull(endpoints) {
            "Endpoint must be specified for etcd"
        }
        require(endpoints is List<*>) {
            "Etcd endpoint must be specified in a list"
        }
        return endpoints.map { it as String }
    }

    fun getRabbitmqConfig(configurationPath: String): RabbitmqConnectionConfig {
        val distributionConfiguration = getConfiguration(configurationPath)
        val rabbitmqConfig = distributionConfiguration["rabbitmq"]
        require(rabbitmqConfig is Map<*, *>) {
            "Configuration for rabbitmq should be defined in a map"
        }
        val username = rabbitmqConfig["username"] as? String ?: System.getenv("ALCHEMIST_RABBITMQ_USERNAME")
        val password = rabbitmqConfig["password"] as? String ?: System.getenv("ALCHEMIST_RABBITMQ_PASSWORD")
        val host = rabbitmqConfig["host"] as? String ?: System.getenv("ALCHEMIST_RABBITMQ_HOST")
        val port = rabbitmqConfig["port"] as? Int ?: System.getenv("ALCHEMIST_RABBITMQ_PORT").toInt()
        return RabbitmqConnectionConfig(username, password, host, port)
    }

    private fun getConfiguration(configurationPath: String): Map<*, *> =
        YamlProvider.from(Path(configurationPath).toUri().toURL())

    data class RabbitmqConnectionConfig(val username: String, val password: String, val host: String, val port: Int)
}
