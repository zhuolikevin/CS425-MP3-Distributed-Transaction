# MP3 Distributed Transactions

- The project building is powered by [Apache Ant](http://ant.apache.org/). If it is at your hand, you can simply run the following command at the root of the project:

    ```bash
    $ ant
    ```

    For Ant installation on Linux, try:
    ```bash
    $ yum install ant
    ```

 This will generate a directory named `dist/`. The complied and packed `.jar` packages are inside.

- Move to `dist/`, boot up nodes in **server**, **coordinator** and **client** order.

    1. Run server. Server name can be A/B/C/D/E

        ```bash
        $ java -jar cs425-mp3-server.jar [server name]
        ```

    2. Run coordinator.

        ```bash
        $ java -jar cs425-mp3-coordinator.jar F
        ```

    3. Run clients.

        ```bash
        $ java -jar cs425-mp3-client.jar 1/2/3
        ```
