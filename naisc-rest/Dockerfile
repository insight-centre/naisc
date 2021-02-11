FROM tomcat:9.0-jdk14-openjdk-oracle

RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY naisc-rest/build/libs/naisc-rest-1.1.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir /usr/local/tomcat/configs/
COPY configs/* /usr/local/tomcat/configs/
RUN mkdir /usr/local/tomcat/models/
COPY models/* /usr/local/tomcat/models/
COPY openapi.yaml /
