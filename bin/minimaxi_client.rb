#!/bin/env jruby

require 'pry'
require 'active_support'
ActiveSupport::Dependencies.autoload_paths = ['./lib/']

if ARGV.length < 1
  puts "Usage: #{$0} <directory_path> [<source id> ...]"
  exit 1
end

dir_path = ARGV[0]
source_ids = ARGV[1..-1]

puts "Directory Path: #{dir_path}"
if source_ids.empty?
  source_ids = nil
  puts "No client source ids, processing all"
else
  puts "Client source ids: #{source_ids.join(', ')}"
end

require_relative '../lib/mercury'
config = {
  "tenant" => "mm",
  "system" => "polsoft",
  "source" => "filesystem",
  "filesystem.path" => dir_path,
  #"redbay.url" => 'http://mm.luxor.aox.pl/api',
  "redbay.url" => 'https://panel.b2b-online.pl/api',
  "redbay.apikey" => '***REMOVED***',
  "redbay.auth_id" => '***REMOVED***',
  "redbay.auth_pass" => '***REMOVED***',
  'polsoft.department' => "1"
}
redbay_client = Mercury::RedbayClient.configure(config)
flow = Mercury::Polsoft.new(config: config, write_transport: nil, redbay_client: redbay_client)
transport = Mercury::Transport::Filesystem.configure(config, readonly: true, mode: 'r:iso-8859-2:utf-8')
flow.sync_selected_clients(transport, source_ids)