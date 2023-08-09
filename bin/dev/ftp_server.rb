#!/bin/env jruby
description = <<~TEXT
  Simple FTP server serving files from local directory
  Usage:
    ./ftp_server.rb  [-r|--root=<path>] [-p|--port=2121]

  Options:

TEXT

require 'ftpd'
require 'tmpdir'
require 'pry'

class FilesystemDriver
  def initialize(dir)
    @dir = dir
  end
  def authenticate(user, password)
    true
  end
  def file_system(user)
    Ftpd::DiskFileSystem.new(@dir)
  end

end

def run_ftpd(dir, port)
  driver = FilesystemDriver.new(dir)
  server = Ftpd::FtpServer.new(driver)
  server.port = port
  server.log = Logger.new(STDOUT)
  server.start
  puts "Server listening on port #{server.bound_port} serving root directory #{dir}"
  server.join
end

if __FILE__ == $PROGRAM_NAME
  require 'optparse'
  args = {port: 2121, dir: Dir.pwd}
  opt_parser = OptionParser.new do |opts|
    opts.banner = description
    opts.on("-r DIR", '--root DIR', "Root directory to serve") { |v|
      args[:dir] = File.expand_path(v, Dir.pwd) }
    opts.on("-p PORT", '--port PORT', "FTP port (default 2121)") { |v| args[:port] = v }
    opts.on("-h", "--help", "Prints this help") do
      puts opts
      exit
    end
  end
  opt_parser.parse!

  if args[:dir]
    run_ftpd(args[:dir], args[:port])
  else
    abort(opt_parser.help)
  end
end
