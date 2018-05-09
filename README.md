Jenkins Logstash Plugin
=======================

Travis: [![Build Status](https://travis-ci.org/jenkinsci/logstash-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/logstash-plugin)
Jenkins: [![Build Status](https://ci.jenkins.io/job/Plugins/job/logstash-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/logstash-plugin/job/master/)

This plugin adds support for sending a job's console log to Logstash indexers such as [Elastic Search](https://www.elastic.co/products/elasticsearch), [Logstash](https://www.elastic.co/de/products/logstash), [RabbitMQ](https://www.rabbitmq.com), [Redis](https://redis.io/) or to Syslog.

* see [Jenkins wiki](https://wiki.jenkins-ci.org/display/JENKINS/Logstash+Plugin) for detailed feature descriptions
* use [JIRA](https://issues.jenkins-ci.org) to report issues / feature requests

Install
=======

* Generate the `hpi` file with the command: `mvn package`

* Put the `hpi` file in the directory `$JENKINS_HOME/plugins`
* Restart jenkins

Configure
=========

Currently supported methods of input/output:

* ElasticSearch {REST API}
* Logstash TCP input
* Redis {format => 'json_event'}
* RabbitMQ {mechanism => PLAIN}
* Syslog {format => cee/json ([RFC-5424](https://tools.ietf.org/html/rfc5424),[RFC-3164](https://tools.ietf.org/html/rfc3164)), protocol => UDP}

Pipeline
========

Logstash plugin can be used as a publisher in pipeline jobs to send the whole log as a single document.

```Groovy
 node('master') {
        sh'''
        echo 'Hello, world!'
        '''
        logstashSend failBuild: true, maxLines: 1000
 }
```

It can be used as a wrapper step to send each log line separately.

Note: when you combine with timestamps step, you should make the timestamps the outer most block. Otherwise you get the timestamps as part of the log lines, basically duplicating the timestamp information. 

```Groovy
timestamps {
  logstash {
    node('somelabel') {
      sh'''
      echo 'Hello, World!'
      '''
    }
  }
}
```

License
=======

The Logstash Plugin is licensed under the MIT License.

Contributing
============

* Fork the project on [Github](https://github.com/jenkinsci/logstash-plugin)
* Make your feature addition or bug fix, write tests, commit.
* Send me a pull request. Bonus points for topic branches.

Adding support for new indexers
-------------------------------

* Implement the extension point `jenkins.plugins.logstash.configuration.LogstashIndexer` that will take your configuration. 
* Implement `equals()` and `hashCode()`so the plugin can compare new configuration with existing configuration.
* Create a `configure-advanced.jelly` for the UI part of your configuration.
* Create a new class that extends `jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao` or `jenkins.plugins.logstash.persistence.HostBasedLogstashIndexer`. This class will do the actual work of pushing the logs to the indexer.
