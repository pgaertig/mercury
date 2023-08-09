require_relative 'lib/mercury/version'
#require 'rspec/core/rake_task'
require 'jars/classpath'
require 'rake/javaextensiontask'

require 'jars/installer'
task :install_jars do
  Jars::Installer.new.vendor_jars
end

desc 'Compiles extension and run specs'
task default: %i[compile]  #i[compile spec]

spec = eval File.read('mercury.gemspec') # rubocop:disable Security/Eval

desc 'compile src/main/java/** into lib/example.jar'
Rake::JavaExtensionTask.new('mercury', spec) do |ext|
  ext.classpath = Jars::Classpath.new.classpath_string
  ext.source_version = '19'
  ext.target_version = '19'
  ext.ext_dir = 'src/main/java'
end

require 'rubygems/package_task'
Gem::PackageTask.new(spec) do
  desc 'Pack gem'
  task package: %i[install_jars compile]
end

#desc 'Run specs'
#RSpec::Core::RakeTask.new

# def create_manifest
#   title =  'Implementation-Title: mercury (Mercury)'
#   version =  format('Implementation-Version: %s', Mercury::VERSION)
#   file = File.open('MANIFEST.MF', 'w') do |f|
#     f.puts(title)
#     f.puts(version)
#   end
# end
#
# task default: [:init, :compile, :test]
#
# desc 'Create Manifest'
# task :init do
#   create_manifest
# end

# desc 'Compile'
# task :compile do
#   sh 'mvn package'
#   sh 'mv target/jruby-ext.jar ../lib'
# end

#desc 'Test'
#task :test do
#  sh 'jruby test/foo_test.rb'
#  sh 'jruby test/bar_spec_test.rb'
#end
#
# desc 'clean'
# task :clean do
#   Dir['./**/*.%w{jar}'].each do |path|
#     puts 'Deleting #{path} ...'
#     File.delete(path)
#   end
#   FileUtils.rm_rf('./target')
# end