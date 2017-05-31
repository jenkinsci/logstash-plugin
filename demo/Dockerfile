FROM jenkinsci/jenkins:2.32

COPY plugins.txt /plugins.txt
RUN /usr/local/bin/plugins.sh /plugins.txt

# See PR links from https://issues.jenkins-ci.org/browse/JENKINS-38381
# TODO replace with Maven snapshot URLs
ADD https://jitpack.io/com/github/jglick/durable-task-plugin/8f11c53/durable-task-plugin-8f11c53.hpi /usr/share/jenkins/ref/plugins/durable-task.jpi 
ADD https://jitpack.io/com/github/jglick/workflow-api-plugin/ea218b9/workflow-api-plugin-ea218b9.hpi /usr/share/jenkins/ref/plugins/workflow-api.jpi
ADD https://jitpack.io/com/github/jglick/workflow-support-plugin/284f0dc/workflow-support-plugin-284f0dc.hpi /usr/share/jenkins/ref/plugins/workflow-support.jpi
ADD https://jitpack.io/com/github/jglick/workflow-job-plugin/2de9add/workflow-job-plugin-2de9add.hpi /usr/share/jenkins/ref/plugins/workflow-job.jpi
ADD https://jitpack.io/com/github/jglick/workflow-durable-task-step-plugin/363bafa/workflow-durable-task-step-plugin-363bafa.hpi /usr/share/jenkins/ref/plugins/workflow-durable-task-step.jpi
ADD https://jitpack.io/com/github/jglick/logstash-plugin/4f22bd3/logstash-plugin-4f22bd3.hpi /usr/share/jenkins/ref/plugins/logstash.jpi

ADD JENKINS_HOME /usr/share/jenkins/ref/

# https://github.com/docker/docker/issues/6119 workaround:
USER root
RUN chown -R jenkins.jenkins /usr/share/jenkins/ref
USER jenkins
