FROM jenkinsci/jenkins:2.89.3

ADD JENKINS_HOME /usr/share/jenkins/ref/

# https://github.com/docker/docker/issues/6119 workaround:
USER root
RUN chown -R jenkins.jenkins /usr/share/jenkins/ref
USER jenkins
