To run this demo:

1. `docker-compose rm -fv && docker-compose up --build --force-recreate jenkins elk`
1. when that is started enough, `docker-compose up -d agent`
1. go to http://localhost:8080/job/sample/
1. **Build Now**
1. inspect **Console Output**
1. see console annotations in `Started by user`__`anonymous`__
1. **hide**/**show** blocks
1. configure http://localhost:5601/ with `logstash*` and `@timestamp`
1. inspect **External log (Kibana)**
1. compare `sender` fields to `docker ps` container IDs
1. verify that passwords are masked
