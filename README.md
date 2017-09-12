
# Stanford CoreNLP Servlet

A Java Servlet for Stanford CoreNLP offering the same interface as [CoreNLP web server](https://stanfordnlp.github.io/CoreNLP/corenlp-server.html)

## Building

`git clone git@github.com:marcocor/stanford-corenlp-servlet.git`

`cd stanford-corenlp-servlet/`

`mvn war:war`

This will produce a WAR package in directory `target/`. Deploy it to your servlet container.

## Calling

This webapp offers the same interface as the CoreNLP web server. Please refer to its [documentation](https://stanfordnlp.github.io/CoreNLP/corenlp-server.html) for details.

Example call (change `http://localhost:8080/stanford-corenlp-servlet` to your webapp endpoint):

`curl --data 'The quick brown fox jumped over the lazy dog.' 'http://localhost:8080/stanford-corenlp-servlet?properties={%22annotators%22%3A%22tokenize%2Cssplit%2Cpos%22%2C%22outputFormat%22%3A%22json%22}' -o -`

## Bug reporting

For any problem, feel free to open an issue on Github.
