def LOGSTASH_DOCKER_IMAGE="logstash/logstash"
def LOGSTASH_DOCKER_REGISTRY="docker.elastic.co"
def LOGSTASH_DOCKER_VERSION="5.4.1" 
def LOGSTASH_DOCKER_ENV="XPACK_MONITORING_ENABLED=false"
def LOGSTASH_PIPELINE_DIR="src/test/resources/logstash/pipeline"
def LOGSTASH_LOGS_DIR="target/logs"
def LOGSTASH_LOGFILE="syslog-test.log"


node('linux') {
      stage('Docker'){
            checkout scm
            docker.withRegistry(LOGSTASH_DOCKER_REGISTRY) { 
                    withEnv(["JAVA_HOME=${tool 'jdk8'}",
                              'PATH+JAVA=${JAVA_HOME}/bin',
                              "PATH+MAVEN=${tool 'mvn'}/bin"]) {
                    def TRAVIS_BUILD_DIR=env.WORKSPACE
                    sh "mvn clean package --quiet"
                    sh "mkdir -p ${TRAVIS_BUILD_DIR}/${LOGSTASH_LOGS_DIR}"
                    sh "touch ${TRAVIS_BUILD_DIR}/${LOGSTASH_LOGS_DIR}/${LOGSTASH_LOGFILE}"
                    sh "sudo chown 1000 ${TRAVIS_BUILD_DIR}/${LOGSTASH_LOGS_DIR}/${LOGSTASH_LOGFILE}"
                    sh "sudo chmod g+w ${TRAVIS_BUILD_DIR}/${LOGSTASH_LOGS_DIR}/${LOGSTASH_LOGFILE}"
                    sh "docker pull ${LOGSTASH_DOCKER_REGISTRY}/${LOGSTASH_DOCKER_IMAGE}:${LOGSTASH_DOCKER_VERSION}"
                    sh """
                        docker run -d \
                      -v ${TRAVIS_BUILD_DIR}/${LOGSTASH_PIPELINE_DIR}:/usr/share/logstash/pipeline \
                      -v ${TRAVIS_BUILD_DIR}/${LOGSTASH_LOGS_DIR}:/tmp/logs \
                      -p 127.0.0.1:514:5555 \
                      -p 127.0.0.1:514:5555/udp \
                      -e ${LOGSTASH_DOCKER_ENV} \
                        ${LOGSTASH_DOCKER_REGISTRY}/${LOGSTASH_DOCKER_IMAGE}:${LOGSTASH_DOCKER_VERSION} \
                     """
                     sh "mvn verify -DskipIntegrationTests=false"
                  }
            }
      }
}

buildPlugin()
