require 'java'
#require 'active_support'
$LOAD_PATH.unshift(__dir__) unless $LOAD_PATH.include?(__dir__)
#ActiveSupport::Dependencies.autoload_paths = [__dir__]
require 'mercury_jars'
require 'log4jruby'


app_fat_jar = Dir["target/mercury*.jar"].first
unless app_fat_jar
  raise("This script needs Spring Boot fat JAR based app available in target/")
end

lib_jars = Dir["./#{app_fat_jar}!/BOOT-INF/lib/*.jar"]
if lib_jars.empty?
  raise("No Spring Boot libraries found in #{app_fat_jar}")
end
lib_jars.sort.each {|jar| require jar}

begin
  # Load classes
  require 'mercury.jar'
rescue LoadError
  $CLASSPATH << 'target/classes'
end

module Kernel
  # Java package short-cut method.
  # @example
  #    java.lang.System
  def pl
    JavaUtilities.get_package_module_dot_format('pl') # stub
  end
end

module Mercury
  java_import pl.amitec.MercuryBoot
  MercuryBoot.new.init
end

require 'mercury/exponential_backoff'
require 'mercury/transport'
require 'mercury/redbay_client'
require 'mercury/hash_cache'
require 'mercury/null_cache'
require 'mercury/polsoft'
