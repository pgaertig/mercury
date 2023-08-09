Gem::Specification.new do |s|
  # this is only needed to retrieve the latest version of jar-dependencies
  # so this can run as integration-test
  version = ENV_JAVA['jar-dependencies.version'] || '0.4.2'

  s.name = 'mercury'
  s.version = '2'
  s.author = 'pgae'
  s.email = ['mail@example.com']
  s.summary = 'gem with jar'
  s.description = 'gem with empty jar and jar dependencies'

  # important so jar-dependencies knows it should look for
  # jar declarations in the requirements section !
  s.platform = 'java'

  s.files << Dir['lib/**/*.rb']
  s.files << 'lib/mercury.jar'
  s.files << Dir['*file']
  s.files << 'mercury.gemspec'

  s.require_path = 'lib'

  s.required_ruby_version = '>= 2.6'

  # constrain the version of jar-dependencies itself
  s.add_runtime_dependency 'jar-dependencies'#, "~> #{version}"

  # the jar declarations
  s.requirements << 'jar commons-net:commons-net, 3.8.0'
  s.requirements << 'jar org.apache.logging.log4j:log4j-core, 2.19.0'
  s.requirements << 'jar org.apache.logging.log4j:log4j-api, 2.19.0'
  s.requirements << 'jar org.apache.logging.log4j:log4j-iostreams, 2.19.0'
  s.requirements << 'jar org.apache.logging.log4j:log4j-jcl, 2.19.0'
  #s.requirements << 'jar org.bouncycastle:bcprov-jdk15on, 1.49'
  #s.requirements << 'jar org.slf4j:slf4j-api, 1.7.7'

  # dependency where some transitive dependency gets excluded (jruby comes
  # with joda-time already bundled and this can cause classloader conflicts.
  # better just use the version of joda-time from jruby and hope it is
  # compatible)
  #s.requirements << 'jar io.dropwizard:dropwizard-logging, 0.8.0-rc5, :exclusions=> [ joda-time:joda-time ]'

  # a jar dependency with a classifier
  #s.requirements << 'jar com.google.protobuf:protobuf-java, 2.2.0, :classifier => lite'

  # needed for the tests
  s.requirements << 'jar junit:junit:4.12, :scope => :test'
  # this is part of the test and assumed to be provided during runtime
  #s.requirements << 'jar org.slf4j:slf4j-simple, 1.7.7, :scope => :provided'

  s.add_development_dependency 'rake'#, '~> 10.3'
  s.add_development_dependency 'rake-compiler'#, '~> 10.3'
  s.add_development_dependency 'rb-readline'#, '~> 10.3'
  #s.add_development_dependency 'rspec'#, '~> 2.14'

  # needed to compile src/main/java/** into lib/example.jar
  #s.add_development_dependency 'rake-compiler' #, '~> 1.2.0'

  # avoids to install it on the fly when jar-dependencies needs it
  s.add_development_dependency 'ruby-maven'#, '~> 3.3', '>= 3.3.8'
  #s.metadata['rubygems_mfa_required'] = 'true'
end