set XALAN_LIBS=.\libs\xalan.jar
set XERCES_LIBS=.\libs\xercesImpl.jar
set LOG4J_LIBS=.\libs\log4j-1.2.16.jar
set MAIN_CLASS=de.fhhannover.inform.trust.xacml.DynamicNetworkPDPStarter

java -cp .\bin:%XALAN_LIBS%;%XERCES_LIBS%;%LOG4J_LIBS% %MAIN_CLASS% %*
