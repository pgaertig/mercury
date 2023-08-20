# this is a generated file, to avoid over-writing it just delete this comment
begin
  require 'jar_dependencies'
rescue LoadError
  require 'commons-logging/commons-logging/1.2/commons-logging-1.2.jar'
  require 'org/apache/logging/log4j/log4j-iostreams/2.19.0/log4j-iostreams-2.19.0.jar'
  require 'org/apache/logging/log4j/log4j-jcl/2.19.0/log4j-jcl-2.19.0.jar'
  require 'org/apache/logging/log4j/log4j-core/2.19.0/log4j-core-2.19.0.jar'
  require 'org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar'
  require 'org/apache/commons/commons-csv/1.10.0/commons-csv-1.10.0.jar'
  require 'commons-net/commons-net/3.8.0/commons-net-3.8.0.jar'
end

if defined? Jars
  require_jar 'commons-logging', 'commons-logging', '1.2'
  require_jar 'org.apache.logging.log4j', 'log4j-iostreams', '2.19.0'
  require_jar 'org.apache.logging.log4j', 'log4j-jcl', '2.19.0'
  require_jar 'org.apache.logging.log4j', 'log4j-core', '2.19.0'
  require_jar 'org.apache.logging.log4j', 'log4j-api', '2.19.0'
  require_jar 'org.apache.commons', 'commons-csv', '1.10.0'
  require_jar 'commons-net', 'commons-net', '3.8.0'
end
