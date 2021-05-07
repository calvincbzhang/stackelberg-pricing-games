***********************************
*INSTRUCTIONS FOR STACKELBERG GAME*
***********************************

(1) cd into the folder containing the project;

(2) run

rmiregistry &

to enable RMI registration;

(3) run

java -classpath poi-3.7-20101029.jar: -Djava.rmi.server.hostname=127.0.0.1 comp34120.ex2.Main &

to run the GUI of the platform;

(4) run

java -Djava.rmi.server.hostname=127.0.0.1 PlayerPolly &

to run the leader for MK1;

OR run

java -Djava.rmi.server.hostname=127.0.0.1 PlayerWeightedOffline &

to run the leader for MK2;

OR run

java -Djava.rmi.server.hostname=127.0.0.1 PlayerSimple &

to run the leader for MK3.