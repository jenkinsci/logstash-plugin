plugins:
	if [ \! -d ../target/test-classes/test-dependencies -o ../pom.xml -nt ../target/test-classes/test-dependencies/index ]; then mvn -f .. -DskipTests clean package; fi
	rm -rf JENKINS_HOME/plugins
	mkdir JENKINS_HOME/plugins
	cp -v ../target/test-classes/test-dependencies/*.hpi ../target/*.hpi JENKINS_HOME/plugins

run: plugins
	docker-compose rm -fv
	docker-compose up --build --force-recreate jenkins elk
