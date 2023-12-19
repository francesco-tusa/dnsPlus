# DNS++: Dynamic Name Resolution with Homomorphic Encryption Based Privacy
*DNS++* is a re-design of the Internet's name resolution system that addresses dynamic information distribution via a *pub/sub*, and provides *privacy* to users interacting with the system through the usage of *homomorphic encryption*.

## Build
Change the current directory to the one containing the project's source and run the command
```console
$ ant jar
```
## Run
Two experiments can be executed with the current *DNS++* code. 

### Experiment 1 
This experiment produces both subscriptions and publications based on randomly generated content for the service names. Different settings can be used for the HEPS parameters, in order to evaluate the impact of both the key length ```n``` and the number of bits ```l``` utilised to represent the service name information.
```console
$ cd ./build/jar
$ java -cp DNSPlus.jar naming.RandomTest
```

### Experiment 2
This experiment uses a list with 1000 of the most popular websites (```websites.txt```), and runs the protocol considering a key length ```n=2048``` bits and a content information size ```l=256``` bits. The average time required for a Broker to add all the entries to its forwarding table is first calculated. Then different notifications are generated using services with different degree of popularity randomly selected from the above list of websites.
```console
$ cd ./build/jar
$ java -cp DNSPlus.jar naming.DNSPlus
```
By default the code will look for the file ```websites.txt``` inside the user's home directory.
