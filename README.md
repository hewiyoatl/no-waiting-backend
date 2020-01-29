# Doc Documents Backend

### Purpose of the Project

#### Business Specs for Documents

### Play modules documentation

``` https://www.playframework.com/documentation/2.8.x/ModuleDirectory#Emailer-Plugin-(Java-and-Scala) ```

### To run the database locally
``` java -classpath lib/hsqldb.jar org.hsqldb.server.Server --database.0 file:hsqldb/nowaiting --dbname.0 nowaiting ```
```  java -classpath lib/hsqldb.jar org.hsqldb.server.Server ```

### install xamp
```https://www.dyclassroom.com/howto-mac/how-to-install-apache-mysql-php-on-macos-mojave-10-14```


### To run the scripts on the database

```  java -cp lib/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing ```

### connect to database
``` ssh -N -L 8888:127.0.0.1:443 -i ~/Desktop/Talachitas/api.pem bitnami@44.231.75.206 ```

#### Build Locally 
```sbt run -Dhttp.port=10002```

#### Run Unit/Integration Test Locally 
```sbt test```

#### Debug purposes (also pointed your intellij to the port 9999 on debug mode) Locally
```sbt -jvm-debug 9999 run```

#### Display the current app once you build successfully 
Then open your browser and point to: http://localhost:9000

#### Configurations
Open the `application.conf` and `routes` file and set your own configurations.

#### Prerequisites
- Scala 2.11
- Play 2.7.2
- Mysql 5.x
- Kafka http://kafka.apache.org/downloads.html
- kafka-manager (https://github.com/yahoo/kafka-manager)

#### Download and install kafka and kafka-manager (kafka server + kafka ui)

http://kafka.apache.org/documentation.html#quickstart

http://docs.confluent.io/3.1.1/kafka-rest/docs/intro.html#quickstart
 
```sudo rm -rf /tmp/kafka-logs```

```sudo rm -rf /tmp/zookeeper```

```bin/zookeeper-server-start.sh config/zookeeper.properties &```    (to start zookeeper)

```bin/kafka-server-start.sh config/server.properties & ``` (start the kafka server)
 
```bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test``` (create a topic)
 
```bin/kafka-topics.sh --list --zookeeper localhost:2181```   (list the topic created)

```bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test```   (send some message to the queue)

```bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic test --from-beginning```   (receive messages from the queue)

#### Prefer to do it all in the confluent kafka restful client: http://docs.confluent.io/2.0.0/kafka-rest/docs/intro.html#installation

```$ ./bin/zookeeper-server-start ./etc/kafka/zookeeper.properties &``` 

```$ ./bin/kafka-server-start ./etc/kafka/server.properties & ```

```$ ./bin/schema-registry-start ./etc/schema-registry/schema-registry.properties & ```

```$ ./bin/kafka-rest-start ./etc/kafka-rest/kafka-rest.properties & ```

Also take a look at these variables on the kafka/server.properties: 

# for auto deleting the topics purposes
delete.topic.enable = true

# for create the topics automatically
auto.create.topics.enable = true

listeners=PLAINTEXT://0.0.0.0:9092

advertised.listeners=PLAINTEXT://0.0.0.0:9092 

Kafka RESTFUL client : http://docs.confluent.io/2.0.0/kafka-rest/docs/intro.html#installation

```bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic my-topic```   my-topic is marked for deletion.

#### Clone the following project (for the web ui kafka, still experimental): 

URL To clone project: https://github.com/yahoo/kafka-manager

```./sbt clean dist```

```cd target/universal/```

```unzip kafka-manager-1.3.1.6.zip```

```cd kafka-manager-1.3.1.6```

(edit the zookeper configuration properties on conf/ directory to point to local) 

```bin/kafka-manager -Dhttp.port=8080 &```

Finally open your browser on:  http://localhost:8080  (kafka manager ui)

#### To generate a war file (this option is discarded in favor of the netty server)

```sbt war```


#### Flyway standalone or sbt (for database management)

```sbt flywayClean```

``` sbt flywayMigrate ```

Or via brew:
 
```brew install flyway```

```flyway clean -X```

```flyway migrate -X```

Then run the following commands: 

```ssh magomez@rlcorpdevweb001 ```

```sudo chef-client```

```sudo rm -rf /opt/releases/current/services/answers-backend/port.txt ```

```ln -s /opt/releases/current/services/answers-backend/answers-backend.jar .```

```nohup java -jar -Dhttp.port=9000 -DDATABASE_URL_DB=jdbc:postgresql://10.1.100.95:5432/rldev1?currentSchema=doc_answer -DUSER_DB=postgres -DUSER_PASSWORD=postgres answers-backend.jar &```

Also we want ran the process once the port.txt was deleted: 

```sudo service rl-answers-backend start```

#### How to generate Docker Image

Requirements: First you will need to have docker server previously 

For docker machines: ```https://docs.docker.com/machine/get-started/```

For docker images: ```https://docs.docker.com/v1.8/userguide/dockerimages/```

For docker manuals: ```https://docs.docker.com/engine/reference/api/docker_remote_api/```


#### To Publish the docker image format to be readable by docker

```sbt docker:publishLocal```


#### The Docker Survival guide: 

```docker-machine ls```  (list how many virtual machines we have with docker)

```docker-machine start``` (start the docker machine)

```docker-machine env default``` (to set the environment variables for the current shell)

```eval "$(docker-machine env default)"``` (to set the variables in the shell)

```docker-machine ip default``` (to get the default ip, workaround )

```docker images```  (to list the images you have on your local docker server)

```docker ps```  (to list the docker container images running)

```docker exec -it 28a7d8b37e7e bash```   (to ssh into an existent docker container part of a docker machine)

```docker run -p 9000:9000 --net=host rl-template-api:0.0.1-SNAPSHOT```  (first way to deploy this app)

```docker run -p 9000:9000 --add-host=localhost:192.168.0.102 rl-template-api:0.0.1-SNAPSHOT```  (second way to deploy this app)

```docker run -p 9000:9000 --name rltemplate  rl-template-api:0.0.1-SNAPSHOT``` (third way to deploy this app)

```docker run -p 9000:9000 --env USER_DB=hespinosa rl-template-api:0.0.1-SNAPSHOT``` (override some variables)

```docker run -p 9000:9000 --env DATABASE_URL_DB="jdbc:postgresql://192.168.99.100:5432/documents?currentSchema=template" --env USER_DB=hespinosa --env root.level=INFO rl-template-api:0.0.1-SNAPSHOT```

```docker logs ae90838ae4b7```  (to check for the standard logs from existent container)

To remove docker containers

```docker stop $(docker ps -a -q)```

```docker rm $(docker ps -a -q)```


#### To remove particular images on the docker containers:

```docker rmi a929bac69534```


#### Delete all docker images
   
```docker rmi $(docker images -q)```


#### Docker Prometheus servers locally

```docker pull prom/prometheus```

```docker run -p 9090:9090 prom/prometheus &```

```docker-machine ip default```

#### Docker Postgres image locally already included the extensions ltree and hstore

```docker pull postgres:9.4.5```

```docker run --name application_db -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres:9.4.5```

#### The Minikube Survival guide: 

```eval $(minikube docker-env)```

```docker images```

```kubectl run template-service --image=rl-template-api:0.0.1-SNAPSHOT --port=9000```

```kubectl run template-service4 --image=rl-template-api:0.0.1-SNAPSHOT --env="DATABASE_URL_DB=jdbc:postgresql://192.168.99.100:5432/documents?currentSchema=template" --env="USER_DB=hespinosa" --env="log_level=WARN" --port=9000```

```kubectl expose deployment template-service4 --type=NodePort```

```kubectl get services```

```minikube service template-service4```

```curl $(minikube ip):9000```

```minikube service template-service4 ```

```kubectl delete deployment template-service4```

#### Cluster key commands

```docker login quay.io```
   
```docker tag 4816a762bfca quay.io/maugomez77/docs-backend:0.0.2-SNAPSHOT```
   
```docker tag 4816a762bfca quay.io/maugomez77/docs-backend:0.0.1.1-SNAPSHOT```
   
``` time docker push quay.io/maugomez77/docs-backend ```

```kubectl describe node rlcorpdock001```

```kubectl describe service postgrespublic```

```kubectl describe po docs-backend-4175817394-6goz0```

```kubectl describe service postgrespublic```

```kubectl expose deployment docs-postgres --name=postgrespublic --type=NodePort --port=5432```
   
```kubectl run docs-postgres --image=postgres:9.4```

```kubectl run docs-backend --image=quay.io/rocketlawyer/docs-backend:0.0.1-SNAPSHOT --env="DATABASE_URL_DB=jdbc:postgresql://postgrespublic:5432/rldev1?currentSchema=template" --env="USER_DB=postgres" --env="USER_PASSWORD=postgres" --port=9000```

```kubectl config use-context Default```

```kubectl expose deployment docs-backend --name=docs-backend --type=NodePort --port=9000```

```kubectl get nodes```    (to know the nodes)

```kubectl describe services docs-backend```  (to know the port)
