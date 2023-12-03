#!/usr/bin/env -S jruby -J--enable-preview -J-Djdk.httpclient.HttpClient.log=errors,requests,headers,content
# -J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044
require 'pry'
require 'json'
logback_path = "#{__dir__}/dev-logback.xml"
java.lang.System.setProperty("logback.configurationFile", logback_path)
java.lang.System.setProperty("slf4j.provider","ch.qos.logback.classic.spi.LogbackServiceProvider")
$LOAD_PATH.unshift(__dir__) unless $LOAD_PATH.include?(__dir__)
begin
  $CLASSPATH << File.read("#{__dir__}/../target/dev-classpath.txt").split(":")
rescue => e
  $stderr.puts e
  $stderr.puts "Run at least `mvn compile` to generate classes"
  exit(1)
end
$CLASSPATH << "#{__dir__}/../target/classes"

module Kernel
  def pl
    JavaUtilities.get_package_module_dot_format('pl') # stub
  end
end

if ARGV.empty?
  puts("Usage: #{$0} <file_path>")
  exit 1
end

logger = Java::OrgSlf4j::LoggerFactory.getLogger java.util.Date.java_class
logger.info("Starting bitbee-client.rb")
config = pl.amitec.mercury.util.StructUtils.propertiesFileToMap(ARGV[0])
client = pl.amitec.mercury.clients.bitbee.BitbeeClient.new(config)
client.session {
  binding.pry
}
