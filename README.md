Jenkins Logstash Plugin
=======================

This plugin adds support for sending a job's console log to Logstash indexers such as ElasticSearch, RabbitMQ, or Redis.

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
* Redis {format => 'json_event'}
* RabbitMQ {mechanism => PLAIN}

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

* Create a new class in the package `jenkins.plugins.logstash.persistence` that extends `AbstractLogstashIndexerDao`
* Add a new entry to the enum `IndexerType` in `LogstashIndexerDao`
* Add a new mapping to the `INDEXER_MAP` in `IndexerDaoFactory`
