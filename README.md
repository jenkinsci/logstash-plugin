Jenkins Logstash Plugin
=======================

Travis: [![Build Status](https://travis-ci.org/jenkinsci/logstash-plugin.svg?branch=master)](https://travis-ci.org/jenkinsci/logstash-plugin)
Jenkins: [![Build Status](https://ci.jenkins.io/job/Plugins/job/logstash-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/logstash-plugin/job/master/)

This plugin adds support for sending a job's console log to Logstash indexers such as [Elastic Search](https://www.elastic.co/products/elasticsearch), 
[Logstash](https://www.elastic.co/de/products/logstash), [RabbitMQ](https://www.rabbitmq.com), 
[Redis](https://redis.io/) or to Syslog.

* use [Jira](https://issues.jenkins.io) to report issues / feature requests

Install
=======

* Search for 'Logstash' in your Jenkins plugin manager

Configure
=========

Supported methods of input/output:

* ElasticSearch (REST API)
* Logstash TCP input
* Redis (format => 'json_event')
* RabbitMQ (mechanism => PLAIN)
* Syslog (format => cee/json ([RFC-5424](https://tools.ietf.org/html/rfc5424),[RFC-3164](https://tools.ietf.org/html/rfc3164)), protocol => UDP)

Pipeline
=========

Publisher
---------

Logstash plugin can be used as a publisher in pipeline jobs to send the
tail of the log as a single document.

**Example for publisher in pipeline**

```Groovy
node('master') {
    sh'''
        echo 'Hello, world!'
    '''
    logstashSend failBuild: true, maxLines: 1000
}
```

Note: Due to the way logging works in pipeline currently, the
logstashSend step might not transfer the lines logged directly before
the step is called. Adding a sleep of 1 second might help here.

Note: In order to get the the result set in pipeline it must be [set
before the logstashSend
step](https://support.cloudbees.com/hc/en-us/articles/218554077-How-to-set-current-build-result-in-Pipeline-).

Note: the `logstashSend` step requires a node to run.

Step with Block
---------------

It can be used as a wrapper step to send each log line separately.

Once the result is set, it will appear in the data sent to the indexer.

Note: when you combine with timestamps step, you should make the
timestamps the outermost block. Otherwise you get the timestamps as
part of the log lines, basically duplicating the timestamp information.

**Example for pipeline step**

```Groovy
timestamps {
  logstash {
    node('somelabel') {
      sh'''
        echo 'Hello, World!'
      '''
      try {
        // do something that fails
        sh "exit 1"
        currentBuild.result = 'SUCCESS'
      } catch (Exception err) {
        currentBuild.result = 'FAILURE'
      }    
    }
  }
}
```

Note: Information on which agent the steps are executed is not available
at the moment.

Enable Globally
=======

You can enable this plugin globally in the Jenkins system configuration page, 
or with the [configuration as code](https://plugins.jenkins.io/configuration-as-code/) plugin: 

```yaml
unclassified:
  logstashConfiguration:
    enableGlobally: true
    enabled: true
    logstashIndexer:
        logstash:
            host: "localhost"
            port: 9200
```

JobProperty
=======

This component streams individual log lines to the indexer for post-processing, 
along with any build data that is available at the start (some information such as the build status is unavailable or incomplete).

Post-Build Publisher
=======

This component pushes the tail of the job's log to the indexer for post-processing, 
along with all build data at the time the post-build action had started (if any post-build actions are scheduled after this plugin they will not be recorded).

JSON Payload Format
=======

Example payload sent to the indexer (e.g. RabbitMQ) using the post-build action component. 

_Note 1: when the build wrapper is used, some information such as the build result will be missing or incomplete, 
and the "message" array will contain a single log line._

_Note 2: `data.testResults` will only be present if a publisher records your test results in the build, 
for example by using the [JUnit Plugin](https://plugins.jenkins.io/junit/)._

<details>

<summary>Click to expand the JSON payload</summary>

```json
{
   "data":{
      "id":"2014-10-13_19-51-29",
      "result":"SUCCESS",
      "projectName":"my_example_job",
      "fullProjectName":"folder/my_example_job",
      "displayName":"#1",
      "fullDisplayName":"My Example Job #1",
      "url":"job/my_example_job/1/",
      "buildHost":"Jenkins",
      "buildLabel":"",
      "buildNum":1,
      "buildDuration":0,
      "rootProjectName":"my_example_job",
      "rootFullProjectName":"folder/my_example_job",
	  "rootProjectDisplayName":"#1",
      "rootBuildNum":1,
      "buildVariables":{
         "PARAM1":"VALUE1",
         "PARAM2":"VALUE2"
      },
      "testResults":{
         "totalCount":45,
         "skipCount":0,
         "failCount":0,
         "failedTests":[]
      }
   },
   "message":[
      "Started by user anonymous",
      "Building in workspace /var/lib/jenkins/jobs/my_example_job/workspace",
      "Hello, World!"
   ],
   "source":"jenkins",
   "source_host":"http://localhost:8080/jenkins/",
   "@timestamp":"2014-10-13T19:51:29-0700",
   "@version":1
}
```

</details>

Changelog
=======

See [Changelog](./CHANGELOG.md).

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
* Create a `help.jelly` with more details about indexer.
* Create a new class that extends `jenkins.plugins.logstash.persistence.AbstractLogstashIndexerDao` or `jenkins.plugins.logstash.persistence.HostBasedLogstashIndexer`. This class will do the actual work of pushing the logs to the indexer.
